// PGH
package com.aio.hospitalsafety.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 화면의 '아이디 확인' 버튼 요청값을 받는 DTO다.
 *
 * 아이디 단독으로는 존재 여부를 확인해 주지 않는다(계정 열거 공격 방지). 아이디와
 * 이름이 함께 일치할 때만 확인된 것으로 응답한다.
 */
public class CheckEmployeeIdForm {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 20, message = "아이디는 20자 이하여야 합니다.")
    private String employeeId;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String employeeName;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
}
