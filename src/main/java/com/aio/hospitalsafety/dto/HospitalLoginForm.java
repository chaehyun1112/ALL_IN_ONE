// PGH
package com.aio.hospitalsafety.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 로그인 1단계에서 사용자가 입력한 병원 구분 ID를 받는 DTO다. */
public class HospitalLoginForm {

    @NotBlank(message = "병원 구분 ID를 입력해 주세요.")
    @Size(max = 30, message = "병원 구분 ID는 30자 이하여야 합니다.")
    private String hospitalId;

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
}
