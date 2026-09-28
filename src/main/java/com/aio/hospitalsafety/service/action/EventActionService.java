package com.aio.hospitalsafety.service.action;

import com.aio.hospitalsafety.common.RoomNumbers;
import com.aio.hospitalsafety.common.SeoulTimes;
import com.aio.hospitalsafety.dto.action.ActionHistoryResponse;
import com.aio.hospitalsafety.dto.action.ActionHistoryRow;
import com.aio.hospitalsafety.dto.action.EventActionRequest;
import com.aio.hospitalsafety.dto.action.EventWard;
import com.aio.hospitalsafety.dto.action.TodayEventResponse;
import com.aio.hospitalsafety.mapper.action.EventActionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 조치기록 조회와 대응 등록.
 * 병원·병동 범위는 브라우저 입력이 아니라 로그인 정보로 정한다.
 */
@Service
public class EventActionService {

    // 조치기록 화면이 한 번에 받는 최대 건수
    private static final int HISTORY_LIMIT = 1000;

    // 조치기록 화면의 종류 이름 (대시보드 상태 이름과 같다)
    private static final String FALL_CONFIRMED_LABEL = "낙상 감지";

    private final EventActionMapper eventActionMapper;

    public EventActionService(EventActionMapper eventActionMapper) {
        this.eventActionMapper = eventActionMapper;
    }

    /**
     * 조치기록 목록.
     * wardId 가 null 이면 병원 전체(관리자), 값이 있으면 그 병동만(간호사).
     */
    @Transactional(readOnly = true)
    public List<ActionHistoryResponse> findActionHistory(String hospitalId, Long wardId) {
        return eventActionMapper.findActionHistory(hospitalId, wardId, HISTORY_LIMIT)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 대시보드를 새로 열었을 때 불러오는 병동의 오늘 감지 이벤트(+ 24시간 안의 조치 없는 확정 낙상, EventActionMapper.xml) */
    @Transactional(readOnly = true)
    public List<TodayEventResponse> findTodayEvents(String hospitalId, Long wardId) {
        return eventActionMapper.findTodayEvents(hospitalId, wardId)
                .stream()
                .map(row -> new TodayEventResponse(
                        row.eventId(),
                        RoomNumbers.forDashboard(row.locationType(), row.locationName()),
                        row.locationName(),
                        SeoulTimes.withOffset(row.eventAt()),
                        row.eventType(),
                        row.decisionSt(),
                        row.handled()))
                .toList();
    }

    /**
     * 대응 등록. 자기 병동에서 난 이벤트에만 등록할 수 있고, 이벤트당 1건이다.
     */
    @Transactional
    public void registerAction(
            String eventId,
            String hospitalId,
            Long userWardId,
            String userId,
            EventActionRequest request) {

        EventWard eventWard = eventActionMapper.findEventWard(eventId);
        if (eventWard == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "감지 이벤트를 찾을 수 없습니다.");
        }
        boolean sameWard = eventWard.hospitalId().equals(hospitalId)
                && eventWard.wardId().equals(userWardId);
        if (!sameWard) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 병동의 이벤트만 조치를 등록할 수 있습니다.");
        }

        int inserted = eventActionMapper.insertEventAction(
                eventId,
                userId,
                request.patientName().trim(),
                request.actionContent().trim());
        if (inserted == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 조치가 등록된 이벤트입니다.");
        }
    }

    private ActionHistoryResponse toResponse(ActionHistoryRow row) {
        boolean done = row.actionAt() != null;
        return new ActionHistoryResponse(
                row.eventId(),
                SeoulTimes.screenMinute(row.eventAt()),
                row.locationName(),
                nullToEmpty(row.patientName()),
                // 조치기록은 확정 낙상만 조회하므로(EventActionMapper.xml) 종류는 항상 "낙상 감지" 다.
                // record.js 가 type === "낙상 감지" 인 줄만 보여 준다.
                FALL_CONFIRMED_LABEL,
                nullToEmpty(row.staffName()),
                done ? "완료" : "미확인",
                SeoulTimes.screenMinute(row.actionAt()),
                nullToEmpty(row.actionContent()),
                nullToEmpty(row.wardName()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
