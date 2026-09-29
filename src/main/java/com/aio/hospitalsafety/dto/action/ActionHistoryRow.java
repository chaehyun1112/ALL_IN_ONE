package com.aio.hospitalsafety.dto.action;

import java.time.OffsetDateTime;

/** 감지 이벤트와 조치(없으면 null)를 DB 에서 그대로 읽은 한 줄 */
public record ActionHistoryRow(
        String eventId,
        String eventType,
        String decisionSt,
        OffsetDateTime eventAt,
        String locationName,
        String patientName,
        String staffName,
        OffsetDateTime actionAt,
        String actionContent,
        // [2026.09.27] 관리자 홈의 병동별 집계용. MyBatis 가 SELECT 순서대로 넣으므로 맨 끝에 둔다.
        String wardName,
        // [2026.09.29 변경] 엣지가 event_detail 에 저장한 사고 영상 주소를 관리자 보관함에서 재생합니다.
        String videoUrl
) {
}
