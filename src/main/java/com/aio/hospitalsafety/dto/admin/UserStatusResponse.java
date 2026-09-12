package com.aio.hospitalsafety.dto.admin;

import com.aio.hospitalsafety.domain.AccountStatus;

public record UserStatusResponse(String userId, AccountStatus accountStatus) {
}
