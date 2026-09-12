package com.aio.hospitalsafety.dto.admin;

import com.aio.hospitalsafety.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
        String userId,
        @NotBlank @Size(max = 20) String userName,
        @NotNull Long wardId,
        @NotNull Role role
) {
}
