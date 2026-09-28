package com.aio.hospitalsafety.dto.action;

import java.time.OffsetDateTime;

/** 오늘 감지 이벤트를 DB 에서 그대로 읽은 한 줄 */
public record TodayEventRow(
        String eventId,
        String eventType,
        String decisionSt,
        OffsetDateTime eventAt,
        String locationName,
        String locationType,
        boolean handled
) {
}
