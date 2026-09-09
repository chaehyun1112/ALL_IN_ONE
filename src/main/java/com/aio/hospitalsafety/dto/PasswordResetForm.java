// PGH
package com.aio.hospitalsafety.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

/**
 * 로그인 전 비밀번호 재설정 화면(password-reset.html)의 입력값을 받는 DTO다.
 *
 * 본인 확인 수단: 직원 ID + 이름 일치 여부만 확인한다.
 * 이메일·휴대전화 등 별도 인증 수단이 DB에 없어서 채택한 최소한의 확인 방식이며,
 * 직원 ID와 정확한 이름을 아는 사람이면 누구나 비밀번호를 바꿀 수 있다는 한계가 있다.
 * (Service에서 아이디 존재 여부와 이름 불일치를 같은 오류 메시지로 묶어
 *  "등록된 아이디인지" 자체가 새어나가지 않도록 한다.)
 */
public class PasswordResetForm {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 20, message = "아이디는 20자 이하여야 합니다.")
    private String employeeId;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String employeeName;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$",
            message = "비밀번호는 영문과 숫자를 포함해 8~32자로 입력해 주세요.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.")
    private String passwordConfirm;

    @AssertTrue(message = "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(passwordConfirm);
    }

    // BCrypt의 72바이트 입력 제한을 한글 등 다중 바이트 문자까지 고려해 검사한다.
    @AssertTrue(message = "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.")
    public boolean isPasswordByteLengthValid() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }
}
