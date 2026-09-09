// PGH
package com.aio.hospitalsafety.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 비밀번호 재설정 2단계에서 이메일로 받은 인증코드 입력값을 받는 DTO다. */
public class PasswordResetCodeForm {

    @NotBlank(message = "인증코드를 입력해 주세요.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증코드는 숫자 6자리입니다.")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
