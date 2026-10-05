package com.aio.hospitalsafety.controller.admin;

import com.aio.hospitalsafety.config.HospitalUserDetails;
import com.aio.hospitalsafety.dto.admin.AccidentVideoUnlockRequest;
import com.aio.hospitalsafety.service.admin.AccidentVideoAccessService;
import com.aio.hospitalsafety.service.edge.EventMediaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * [2026.09.29 병합] 관리자 사고 영상 보관함(accident-records.js)이 재생하는 낙상 전후 영상.
 *
 * GET /api/admin/events/{eventId}/media
 * - /api/admin/** 는 SecurityConfig 에서 관리자(ADMIN)만 들어올 수 있다. 여기서는 로그인한 관리자의 병원 영상인지 한 번 더 본다.
 * - 영상은 젯슨이 올린 파일(TB_EVENT_MEDIA, READY)이다. mp4 는 브라우저가 바로 재생한다(예전 avi 는 내려받기만 될 수 있다).
 * - 확인·오경보로 처리된 낙상은 영상이 이미 지워져 404 다.
 * - ResponseEntity&lt;Resource&gt; 라서 브라우저가 구간 요청(Range)을 보내면 스프링이 알아서 나눠 보낸다(재생 위치 이동).
 * - [2026.10.01 추가] 보관함을 열 때 현재 관리자 비밀번호를 다시 확인한다(POST /api/admin/accident-videos/unlock).
 *   확인하지 않았거나 허용 시간(10분)이 지나면 영상과 확인 기록 요청을 403 으로 막는다(AccidentVideoAccessService).
 */
@RestController
public class AdminEventMediaController {

    private final EventMediaService eventMediaService;
    private final AccidentVideoAccessService accidentVideoAccessService;

    public AdminEventMediaController(EventMediaService eventMediaService,
                                     AccidentVideoAccessService accidentVideoAccessService) {
        this.eventMediaService = eventMediaService;
        this.accidentVideoAccessService = accidentVideoAccessService;
    }

    /**
     * [2026.10.01 추가] 사고 영상 보관함 비밀번호 확인.
     * 204: 맞음(10분 동안 재생 허용) / 400: 틀림 / 429: 여러 번 틀려 잠시 확인을 막음
     */
    @PostMapping("/api/admin/accident-videos/unlock")
    public ResponseEntity<Map<String, String>> unlock(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin,
            @RequestBody(required = false) AccidentVideoUnlockRequest request,
            HttpSession session) {
        if (loginAdmin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 로그인 정보가 없습니다.");
        }
        String password = request == null ? null : request.password();
        return switch (accidentVideoAccessService.unlock(
                session, loginAdmin.getHospitalId(), loginAdmin.getUsername(), password)) {
            case UNLOCKED -> ResponseEntity.noContent().build();
            case WRONG_PASSWORD -> ResponseEntity.badRequest()
                    .body(Map.of("message", "비밀번호가 일치하지 않습니다."));
            case LOCKED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "비밀번호를 여러 번 틀려 "
                            + accidentVideoAccessService.lockMinutes() + "분 뒤에 다시 시도할 수 있습니다."));
        };
    }

    @GetMapping("/api/admin/events/{eventId}/media")
    public ResponseEntity<Resource> clip(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin,
            @PathVariable String eventId,
            HttpSession session) {
        if (loginAdmin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 로그인 정보가 없습니다.");
        }
        requireUnlocked(session);
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이벤트 ID 형식이 아닙니다.");
        }
        Path file = eventMediaService.findClipFile(eventId, loginAdmin.getHospitalId());
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 영상이 없습니다.");
        }
        MediaType type = file.getFileName().toString().endsWith(".mp4")
                ? MediaType.parseMediaType("video/mp4")
                : MediaType.parseMediaType("video/x-msvideo");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(type)
                .body(new FileSystemResource(file));
    }

    /**
     * [2026.09.29] 영상 보관함에서 재생 버튼을 처음 눌렀을 때 accident-records.js 가 부른다. 그 영상을 '확인 완료'로 남긴다.
     * 204: 기록함(또는 이미 확인됨) / 404: 이 병원의 저장된 영상이 없음(확인·오경보로 지워졌거나 다른 병원)
     */
    @PostMapping("/api/admin/events/{eventId}/media/viewed")
    public ResponseEntity<Void> viewed(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin,
            @PathVariable String eventId,
            HttpSession session) {
        if (loginAdmin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 로그인 정보가 없습니다.");
        }
        requireUnlocked(session);
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이벤트 ID 형식이 아닙니다.");
        }
        if (!eventMediaService.markViewed(eventId, loginAdmin.getHospitalId(), loginAdmin.getUsername())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 영상이 없습니다.");
        }
        return ResponseEntity.noContent().build();
    }

    /** [2026.10.01 추가] 보관함 비밀번호 확인을 하지 않았거나 허용 시간이 지났으면 403 */
    private void requireUnlocked(HttpSession session) {
        if (!accidentVideoAccessService.isUnlocked(session)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "사고 영상 보관함 비밀번호 확인이 필요합니다.");
        }
    }
}
