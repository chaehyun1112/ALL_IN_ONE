package com.aio.hospitalsafety.dto.admin;

/**
 * [2026.10.01 추가] 사고 영상 보관함을 열 때 보내는 현재 관리자 비밀번호.
 */
public record AccidentVideoUnlockRequest(
        String password
) {
}
