package com.aio.hospitalsafety.dto.action;

/**
 * 대시보드를 새로 열었을 때 불러오는 오늘(한국 시각) 감지 이벤트 한 건.
 * [2026.09.27] 조치 없는 확정 낙상은 24시간 안이면 어제 것도 온다.
 * 실시간 알림(DashboardEventMessage)과 같은 이름을 쓰고, 조치 등록 여부(handled)를 더한다.
 *
 * room 은 병실(ROOM)일 때만 숫자이고, 공용 공간이면 null 이다.
 */
public record TodayEventResponse(
        String id,
        Integer room,
        String locationName,
        String occurredAt,
        String eventType,
        String decisionSt,
        boolean handled
) {
}
