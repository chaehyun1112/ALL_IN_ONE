// PGH
package com.aio.hospitalsafety.dto;

/** 아이디 찾기 인증코드 확인 AJAX 요청의 JSON 응답이다. userId는 status가 SUCCESS일 때만 값이 있다. */
public record FindIdVerifyCodeResponse(String status, String message, String userId) {
}
