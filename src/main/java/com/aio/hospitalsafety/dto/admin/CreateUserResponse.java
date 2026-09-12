package com.aio.hospitalsafety.dto.admin;

import com.aio.hospitalsafety.domain.AccountStatus;
import com.aio.hospitalsafety.domain.Role;

public record CreateUserResponse(
        String userId,
        String userName,
        Long wardId,
        Role role,
        AccountStatus accountStatus,
        boolean mustChangePassword,
        String temporaryPassword
) {
}
