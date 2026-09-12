// [CODEX 생성 파일] Backend A 계정 발급 및 상태 관리 작업을 위해 추가했습니다.
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
