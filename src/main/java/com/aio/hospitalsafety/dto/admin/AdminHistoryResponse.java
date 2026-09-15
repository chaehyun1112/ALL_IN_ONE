package com.aio.hospitalsafety.dto.admin;

import java.time.Instant;

/**
 * 관리자 감사 로그 조회 결과를 화면에 전달한다.
 *
 * TB_ADMIN_HISTORY에는 관리자 ID와 대상 직원 ID를 기록하고,
 * 직원 이름과 담당 병동 이름은 조회 시 TB_EMP와 TB_WARD에서 가져온다.
 *
 * 삭제된 직원은 TB_EMP에서 조회되지 않을 수 있으므로
 * userName과 wardName은 null일 수 있다.
 */
public record AdminHistoryResponse(
        Long historyId,
        String adminId,
        String userId,
        String userName,
        String wardName,
        String actionCode,
        Instant createdAt
) {
}