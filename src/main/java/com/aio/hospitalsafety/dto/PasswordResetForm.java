// PGH
package com.aio.hospitalsafety.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 1단계(본인 확인) 입력값을 받는 DTO다.
 *
 * 본인 확인 수단: 직원 ID + 이름 + 이메일이 모두 일치해야 한다(회원가입 시 등록한 값 기준).
 * 세 값이 모두 일치할 때만 등록된 이메일로 인증코드를 보내며, 어느 값이 틀렸는지는
 * 알려주지 않는다(계정 존재 여부가 새어나가지 않도록).
 */
public class PasswordResetForm {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 20, message = "아이디는 20자 이하여야 합니다.")
    private String employeeId;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String employeeName;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String email;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
