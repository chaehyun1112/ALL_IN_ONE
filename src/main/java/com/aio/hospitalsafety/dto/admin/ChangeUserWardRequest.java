package com.aio.hospitalsafety.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChangeUserWardRequest(

        @NotNull(message = "변경할 병동을 선택해 주세요.")
        @Positive(message = "올바른 병동을 선택해 주세요.")
        Long wardId

) {
}