// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.mapper.UserMapper;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * 아이디 찾기(이메일 인증) 기능을 담당한다.
 *
 * 처리 순서
 * 1. 병원 구분 ID + 이름 + 이메일이 모두 일치하는 계정이 정확히 하나인지 확인한다.
 * 2. 일치하면 6자리 인증코드를 만들어 해당 이메일로 발송한다.
 * 3. 실제 코드 검증과 만료 처리는 FindIdController가 HttpSession에 저장해 담당한다
 *    (이 Service는 상태를 갖지 않는다).
 */
@Service
public class FindIdService {

    private final UserMapper userMapper;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public FindIdService(UserMapper userMapper, MailService mailService) {
        this.userMapper = userMapper;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public FindIdRequestResult requestVerificationCode(String hospitalId, String employeeName, String email) {
        List<String> candidates = userMapper.findMatchingUserIdsForFindId(hospitalId, employeeName, email);
        // 0건(불일치)과 2건 이상(모호한 일치)을 구분하지 않고 같은 결과로 묶는다.
        if (candidates.size() != 1) {
            return FindIdRequestResult.notFound();
        }

        String code = generateSixDigitCode();
        try {
            mailService.sendFindIdVerificationCode(email, code);
        } catch (MailException exception) {
            return FindIdRequestResult.sendFailed();
        }

        return FindIdRequestResult.success(candidates.get(0), code);
    }

    private String generateSixDigitCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /** 인증코드 요청 결과. code는 컨트롤러가 세션에 저장해 이후 검증에 사용한다. */
    public record FindIdRequestResult(Status status, String userId, String code) {
        public enum Status { SUCCESS, NOT_FOUND, SEND_FAILED }

        public static FindIdRequestResult success(String userId, String code) {
            return new FindIdRequestResult(Status.SUCCESS, userId, code);
        }

        public static FindIdRequestResult notFound() {
            return new FindIdRequestResult(Status.NOT_FOUND, null, null);
        }

        public static FindIdRequestResult sendFailed() {
            return new FindIdRequestResult(Status.SEND_FAILED, null, null);
        }
    }
}
