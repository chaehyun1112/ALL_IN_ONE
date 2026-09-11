package com.aio.hospitalsafety.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.config.HospitalUserDetails;
import com.aio.hospitalsafety.dto.WardOption;
import com.aio.hospitalsafety.dto.admin.ApprovedUserResponse;
import com.aio.hospitalsafety.dto.admin.ChangeUserWardRequest;
import com.aio.hospitalsafety.dto.admin.InactiveUserResponse;
import com.aio.hospitalsafety.service.UserSessionService;
import com.aio.hospitalsafety.service.admin.AdminUserManagementService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;
    private final UserSessionService userSessionService;

    public AdminUserManagementController(
            AdminUserManagementService adminUserManagementService,
            UserSessionService userSessionService
    ) {
        this.adminUserManagementService = adminUserManagementService;
        this.userSessionService = userSessionService;
    }

    // 승인 완료 사용자 목록 조회
    @GetMapping("/users/approved")
    public List<ApprovedUserResponse> getApprovedUsers(
            @RequestParam(defaultValue = "") String keyword,
            HttpSession session
    ) {
        String hospitalDomain = requireHospitalDomain(session);

        return adminUserManagementService.getApprovedUsers(
                hospitalDomain,
                keyword
        );
    }

    // 현재 병원의 병동 목록 조회
    @GetMapping("/wards")
    public List<WardOption> getWards(HttpSession session) {
        String hospitalDomain = requireHospitalDomain(session);

        return adminUserManagementService.getWards(hospitalDomain);
    }

    // 승인 완료 사용자의 담당 병동 변경
    @PatchMapping("/users/{userId}/ward")
    public ResponseEntity<Map<String, String>> changeUserWard(
            @PathVariable("userId") String userId,
            @Valid @RequestBody ChangeUserWardRequest request,
            HttpSession session
    ) {
        String hospitalDomain = requireHospitalDomain(session);

        try {
            adminUserManagementService.changeUserWard(
                    hospitalDomain,
                    userId,
                    request.wardId()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "담당 병동이 변경되었습니다."
                    )
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            exception.getMessage()
                    )
            );
        }
    }

    // 계정 비활성화: APPROVED에서 INACTIVE로 변경하고 기존 세션 만료
    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable("userId") String userId,
            HttpSession session
    ) {
        String hospitalDomain = requireHospitalDomain(session);

        try {
            // DB 상태를 INACTIVE로 변경한다.
            adminUserManagementService.deactivateUser(
                    hospitalDomain,
                    userId
            );

            // DB 변경이 성공한 뒤 해당 사용자의 기존 로그인 세션을 만료시킨다.
            userSessionService.expireUserSessions(
                    hospitalDomain,
                    userId
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "계정이 비활성화되었습니다."
                    )
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            exception.getMessage()
                    )
            );
        }
    }

    @GetMapping("/users/inactive")
    public List<InactiveUserResponse> getInactiveUsers(HttpSession session) {
        return adminUserManagementService.getInactiveUsers(requireHospitalDomain(session));
    }

    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable String userId, HttpSession session) {
        String hospitalDomain = requireHospitalDomain(session);
        try {
            adminUserManagementService.activateUser(hospitalDomain, userId);
            return ResponseEntity.ok(Map.of("message", "계정이 활성화되어 승인완료 목록으로 이동했습니다."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}/inactive")
    public ResponseEntity<Map<String, String>> deleteInactiveUser(@PathVariable String userId, HttpSession session) {
        String hospitalDomain = requireHospitalDomain(session);
        try {
            adminUserManagementService.deleteInactiveUser(hospitalDomain, userId);
            userSessionService.expireUserSessions(hospitalDomain, userId);
            return ResponseEntity.ok(Map.of("message", "계정이 삭제되었습니다."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "연결된 업무 기록으로 인해 삭제할 수 없습니다. 관련 기록을 확인해 주세요."));
        }
    }

    // 접속 병원과 인증된 관리자의 소속 병원이 같아야 한다.
    private String requireHospitalDomain(HttpSession session) {
        String hospitalDomain = (String) session.getAttribute(
                SessionConstants.HOSPITAL_DOMAIN
        );

        if (hospitalDomain == null || hospitalDomain.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "병원 접속 정보가 없습니다."
            );
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof HospitalUserDetails admin) || !hospitalDomain.equals(admin.getHospitalId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인한 관리자의 병원만 관리할 수 있습니다.");
        }
        return hospitalDomain;
    }
}
