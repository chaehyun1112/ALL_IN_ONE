package com.aio.hospitalsafety.service.sms;

import com.aio.hospitalsafety.common.RoomNumbers;
import com.aio.hospitalsafety.common.SeoulTimes;
import com.aio.hospitalsafety.dto.sms.FallSmsRequest;
import com.aio.hospitalsafety.dto.sms.SmsRecipient;
import com.aio.hospitalsafety.mapper.sms.SmsMapper;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 확정 낙상 SMS 발송 (요구사항 AIO_035, FR-AD-600, FR-AD-601).
 *
 * 처리 순서
 * 1. 받는 사람을 찾는다. 해당 병동의 활성 직원(GENERAL) 전원 + 병실이면 그 병실 담당 간병인.
 * 2. 요구사항 정의서의 형식으로 문자 내용을 만든다.
 * 3. 한 사람에게 1회씩 SOLAPI 로 보내고 성공·실패를 TB_SMS_SEND_HISTORY 에 기록한다.
 *    이미 기록이 있는 사람은 건너뛴다. 실패해도 다시 보내지 않는다.
 *    (서버가 도중에 꺼져 일부만 보냈으면, 다시 켤 때 SmsRecoveryService 가 나머지 사람에게 보낸다)
 *
 * 낙상 의심과 침대 이탈에는 이 서비스를 부르지 않는다.
 *
 * SOLAPI 키, 비밀키, 발신번호는 코드에 넣지 않고 환경변수로 받는다(application.properties 참고).
 * 셋 중 하나라도 없으면 실제로 보내지 않고 실패로 기록한다.
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final SmsMapper smsMapper;
    private final String senderNumber;

    // SOLAPI 설정이 없으면 null 이다. null 이면 보내지 않고 실패로 기록한다.
    private final DefaultMessageService messageService;

    // 지금 SMS 를 보내고 있는 이벤트 ID. 같은 이벤트를 두 작업이 동시에 보내 중복 문자가 가는 것을 막는다.
    // (예: 서버가 켜지는 순간 젯슨이 보낸 낙상과, 켜질 때 도는 SmsRecoveryService 가 겹치는 경우)
    private final Set<String> sendingEventIds = ConcurrentHashMap.newKeySet();

    public SmsService(
            SmsMapper smsMapper,
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret,
            @Value("${solapi.sender-number:}") String senderNumber) {
        this.smsMapper = smsMapper;
        // 발신번호는 "010-1234-5678" 처럼 넣어도 되게 숫자만 남긴다(SOLAPI 는 숫자만 받는다).
        this.senderNumber = senderNumber.replace("-", "").trim();

        if (apiKey.isBlank() || apiSecret.isBlank() || senderNumber.isBlank()) {
            this.messageService = null;
            log.warn("SOLAPI 설정(키, 비밀키, 발신번호)이 없어 낙상 SMS 는 보내지 않고 실패로 기록합니다.");
        } else {
            this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
        }
    }

    /**
     * 확정 낙상 SMS 를 보낸다.
     *
     * 이 메서드는 예외를 밖으로 던지지 않는다. SMS 가 실패해도 대시보드 알림은 정상으로
     * 나가야 하기 때문이다(AIO_035 예외 흐름).
     * @Async: 따로 도는 작업으로 실행해서 젯슨에게 보내는 응답을 늦추지 않는다(AsyncConfig).
     */
    @Async
    public void sendFallSms(FallSmsRequest request) {
        String eventId = request.eventId().toString();

        // 같은 이벤트를 다른 작업이 보내고 있으면 그쪽에 맡긴다.
        if (!sendingEventIds.add(eventId)) {
            return;
        }
        try {
            List<SmsRecipient> recipients = findRecipients(request);
            if (recipients.isEmpty()) {
                // 받는 사람이 없으면 TB_SMS_SEND_HISTORY 에 넣을 사람(USER_ID, PHONE_NO NOT NULL)이 없어 로그만 남긴다.
                // 확정 낙상인데 아무도 문자를 못 받는 상황이라 ERROR 로 남긴다(간호사 전화번호 미등록 등).
                log.error("낙상 SMS 를 받을 사람이 없습니다. eventId={} 위치={} {}",
                        eventId, request.wardName(), request.locationName());
                return;
            }
            sendToEachOnce(eventId, recipients, buildFallMessage(request));
        } catch (RuntimeException exception) {
            // 예외 전체를 남기면 DB 오류 설명에 전화번호가 섞일 수 있어 종류만 남긴다.
            log.error("낙상 SMS 처리 중 오류가 났습니다. eventId={} 오류={}",
                    eventId, exception.getClass().getSimpleName());
        } finally {
            sendingEventIds.remove(eventId);
        }
    }

    /** 받는 사람: 병동의 활성 직원 전원 + 병실이면 그 병실 담당 간병인 (전화번호가 있는 사람만) */
    private List<SmsRecipient> findRecipients(FallSmsRequest request) {
        List<SmsRecipient> recipients = new ArrayList<>(
                smsMapper.findWardUsers(request.hospitalId(), request.wardId()));

        if (request.room()) {
            // tb_emp.room_no 에는 "301" 처럼 숫자만 저장돼 있다. 위치 이름 "301호" 앞의 숫자를 쓴다.
            // 대시보드 알림과 같은 규칙(RoomNumbers)을 써서 두 곳의 병실 번호가 어긋나지 않게 한다.
            String roomNumber = RoomNumbers.fromLocationName(request.locationName());
            if (roomNumber != null) {
                recipients.addAll(smsMapper.findRoomCaregivers(request.hospitalId(), roomNumber));
            }
        }
        return recipients;
    }

    /**
     * 한 사람씩 1회 보내고 결과를 기록한다.
     * 이 사람에게 이미 기록(성공·실패)이 있으면 건너뛴다. 실패한 사람에게 다시 보내지 않는다(FR-AD-601).
     * 한 사람에서 DB 오류가 나도 나머지 사람에게는 계속 보낸다.
     */
    private void sendToEachOnce(String eventId, List<SmsRecipient> recipients, String content) {
        for (SmsRecipient recipient : recipients) {
            try {
                if (smsMapper.existsSmsHistory(eventId, recipient.userType(), recipient.userId())) {
                    continue;
                }
                boolean sent = sendOne(recipient, content);
                smsMapper.insertSmsHistory(
                        eventId,
                        recipient.userType(),
                        recipient.userId(),
                        recipient.phoneNumber(),
                        content,
                        sent ? "SUCCESS" : "FAILED"
                );
            } catch (RuntimeException exception) {
                // 사람 ID(간병인은 ID 에 전화번호가 들어 있다)와 예외 설명은 로그에 남기지 않는다.
                log.error("낙상 SMS 한 사람 처리 실패, 다음 사람으로 넘어갑니다. eventId={} 구분={} 오류={}",
                        eventId, recipient.userType(), exception.getClass().getSimpleName());
            }
        }
    }

    /**
     * 요구사항 정의서 AIO_035 의 문자 형식.
     *
     * [ALLEYES 안전 알림]
     * 1병동 301호에서 낙상이 감지되었습니다.
     * 즉시 발생 위치를 확인해 주세요.
     * 발생 시각 : 2026.09.21 14:30
     */
    public String buildFallMessage(FallSmsRequest request) {
        return "[ALLEYES 안전 알림]\n"
                + request.wardName() + " " + request.locationName() + "에서 낙상이 감지되었습니다.\n"
                + "즉시 발생 위치를 확인해 주세요.\n"
                + "발생 시각 : " + SeoulTimes.smsMinute(request.eventAt());
    }

    /** SOLAPI 설정(키, 비밀키, 발신번호)이 모두 있어 실제로 보낼 수 있으면 true */
    public boolean isConfigured() {
        return messageService != null;
    }

    /**
     * 한 사람에게 보낸다. 성공하면 true.
     * 전화번호는 로그에 남기지 않는다. 간병인은 사람 ID(cg.전화번호)에도 번호가 있어 받는 사람 구분만 남긴다.
     */
    private boolean sendOne(SmsRecipient recipient, String content) {
        if (messageService == null) {
            log.warn("SOLAPI 설정이 없어 보내지 않았습니다. 구분={}", recipient.userType());
            return false;
        }

        Message message = new Message();
        message.setFrom(senderNumber);
        message.setTo(recipient.phoneNumber().replace("-", ""));
        message.setText(content);

        try {
            messageService.send(message);
            return true;
        } catch (Exception | LinkageError exception) {
            // TB_SMS_SEND_HISTORY 에는 실패 사유를 적을 칸이 없어서 사유(예외 종류)는 로그에만 남긴다.
            // 예외 설명에는 받는 번호가 들어갈 수 있어 남기지 않는다.
            // LinkageError: SOLAPI 라이브러리가 쓰는 OkHttp 가 다른 버전과 부딪치는 경우. 실패로 기록하고 다음 사람으로 넘어간다.
            log.warn("낙상 SMS 발송 실패. 구분={} 사유={}",
                    recipient.userType(), exception.getClass().getSimpleName());
            return false;
        }
    }
}
