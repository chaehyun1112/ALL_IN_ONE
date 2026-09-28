package com.aio.hospitalsafety.dto.action;

/**
 * 조치기록 화면(record.js)이 받는 한 줄. 화면 계약에 맞춰 모두 문자열이다.
 *
 * occurredAt, completedAt: "2026-09-25T14:30" (한국 시각, 없으면 "")
 * type: "낙상 감지" (조치기록은 확정 낙상만 돌려준다)
 * status: "완료"(조치 등록됨) / "미확인"
 */
public record ActionHistoryResponse(
        String eventId,
        String occurredAt,
        String room,
        String patient,
        String type,
        String staff,
        String status,
        String completedAt,
        String actionContent,
        // [2026.09.27] 병동 이름(예: "3병동"). 관리자 홈의 병동별 사고 현황에 쓴다. record.js 는 쓰지 않는다.
        String wardName
) {
}
