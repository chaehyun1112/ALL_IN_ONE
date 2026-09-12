// [CODEX 생성 파일] Backend A 계정 발급 및 상태 관리 작업을 위해 추가했습니다.
package com.aio.hospitalsafety.dto.admin;

import com.aio.hospitalsafety.domain.AccountStatus;

public record UserStatusResponse(String userId, AccountStatus accountStatus) {
}
