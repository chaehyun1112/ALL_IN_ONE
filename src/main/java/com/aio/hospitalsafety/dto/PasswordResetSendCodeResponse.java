// PGH
package com.aio.hospitalsafety.dto;

/** 비밀번호 재설정 인증코드 발송 AJAX 요청의 JSON 응답이다. */
public record PasswordResetSendCodeResponse(String status, String message) {
}
