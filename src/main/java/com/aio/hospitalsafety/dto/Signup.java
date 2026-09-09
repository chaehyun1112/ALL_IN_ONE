package com.aio.hospitalsafety.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record Signup(

        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(max = 20, message = "아이디는 20자 이하여야 합니다.")
        String userId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$",
                message = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        String passwordConfirm,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        String userName,

        @Positive(message = "올바른 병동을 선택해 주세요.")
        Long wardId

) {
}