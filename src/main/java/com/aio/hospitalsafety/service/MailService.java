// PGH
package com.aio.hospitalsafety.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 실제 메일 발송(SMTP)을 담당한다.
 *
 * 발신 계정 정보는 application.properties의 spring.mail.* 값을 통해서만 주입되며,
 * 그 값 자체는 소스 코드가 아닌 실행 환경의 MAIL_USERNAME/MAIL_PASSWORD 환경변수에서 온다.
 * spring.mail.username이 비어 있으면(메일 미설정 개발 환경) SMTP 인증 단계에서
 * MailException이 발생하고, 호출한 Service가 이를 "발송 실패"로 처리한다.
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /** 아이디 찾기 인증코드를 발송한다. 실패하면 MailException을 그대로 던진다(호출자가 처리). */
    public void sendFindIdVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("[늘푸른요양병원] 아이디 찾기 인증코드");
        message.setText(
                "요청하신 아이디 찾기 인증코드는 [" + code + "] 입니다.\n"
                        + "인증코드는 5분간 유효합니다.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시해 주세요.");

        mailSender.send(message);
    }
}
