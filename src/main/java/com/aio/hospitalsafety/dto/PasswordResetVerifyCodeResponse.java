// PGH
package com.aio.hospitalsafety.dto;

/** 비밀번호 재설정 인증코드 확인 AJAX 요청의 JSON 응답이다. */
public record PasswordResetVerifyCodeResponse(String status, String message) {
}
