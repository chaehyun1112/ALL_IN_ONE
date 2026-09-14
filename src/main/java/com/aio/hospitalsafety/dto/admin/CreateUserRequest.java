package com.aio.hospitalsafety.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "직원 아이디를 입력해 주세요.")
        @Size(max = 20, message = "직원 아이디는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다."
        )
        String userId,

        @NotBlank(message = "직원 이름을 입력해 주세요.")
        @Size(max = 20, message = "직원 이름은 20자 이하여야 합니다.")
        String userName,

        @NotNull(message = "담당 병동을 선택해 주세요.")
        Long wardId
) {
}