// PGH
package com.aio.hospitalsafety.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 아이디 찾기 1단계(find-id.html)의 입력값을 받는 DTO다.
 *
 * 본인 확인 수단: 이름 + 이메일 일치 여부만 확인한다(회원가입 시 등록한 값 기준).
 * 일치하는 계정이 정확히 하나일 때만 해당 이메일로 인증코드를 발송하며,
 * 계정이 없거나 여러 건이 모호하게 일치하는 경우도 같은 안내 메시지로 묶어
 * "등록된 이메일인지" 자체가 새어나가지 않도록 한다.
 */
public class FindIdRequestForm {

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String employeeName;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String email;

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
