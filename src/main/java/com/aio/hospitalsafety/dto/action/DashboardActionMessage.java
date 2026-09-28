package com.aio.hospitalsafety.dto.action;

/**
 * [2026.09.27] "이 경보에 조치가 등록됐다"는 대시보드 실시간 알림.
 * 같은 병동 대시보드를 여러 대 열어 둔 경우, 한 화면에서 대응 등록을 하면 다른 화면의 같은 경보도 끈다.
 * 감지 알림(DashboardEventMessage)과 구분하려고 eventType 없이 id 와 handled=true 만 보낸다.
 */
public record DashboardActionMessage(
        String id,
        boolean handled
) {
}
