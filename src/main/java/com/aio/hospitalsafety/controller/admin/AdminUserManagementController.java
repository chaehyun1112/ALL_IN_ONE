package com.aio.hospitalsafety.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.dto.WardOption;
import com.aio.hospitalsafety.dto.admin.ApprovedUserResponse;
import com.aio.hospitalsafety.dto.admin.ChangeUserWardRequest;
import com.aio.hospitalsafety.service.admin.AdminUserManagementService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(
            AdminUserManagementService adminUserManagementService
    ) {
        this.adminUserManagementService =
                adminUserManagementService;
    }

    // 승인 완료 사용자 목록 조회
    @GetMapping("/users/approved")
    public List<ApprovedUserResponse> getApprovedUsers(
            @RequestParam(defaultValue = "") String keyword,
            HttpSession session
    ) {
        String hospitalDomain =
                requireHospitalDomain(session);

        return adminUserManagementService.getApprovedUsers(
                hospitalDomain,
                keyword
        );
    }

    // 현재 병원의 병동 목록 조회
    @GetMapping("/wards")
    public List<WardOption> getWards(
            HttpSession session
    ) {
        String hospitalDomain =
                requireHospitalDomain(session);

        return adminUserManagementService.getWards(
                hospitalDomain
        );
    }

    // 승인 완료 사용자의 담당 병동 변경
    @PatchMapping("/users/{userId}/ward")
    public ResponseEntity<Map<String, String>> changeUserWard(
            @PathVariable("userId") String userId,
            @Valid @RequestBody ChangeUserWardRequest request,
            HttpSession session
    ) {
        String hospitalDomain =
                requireHospitalDomain(session);

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

    // 계정 비활성화: APPROVED에서 PENDING으로 변경
    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable("userId") String userId,
            HttpSession session
    ) {
        String hospitalDomain =
                requireHospitalDomain(session);

        try {
            adminUserManagementService.deactivateUser(
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

    // 세션에서 현재 병원 도메인 확인
    private String requireHospitalDomain(
            HttpSession session
    ) {
        String hospitalDomain =
                (String) session.getAttribute(
                        SessionConstants.HOSPITAL_DOMAIN
                );

        if (hospitalDomain == null
                || hospitalDomain.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "병원 접속 정보가 없습니다."
            );
        }

        return hospitalDomain;
    }
}