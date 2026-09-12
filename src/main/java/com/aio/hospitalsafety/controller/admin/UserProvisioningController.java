// [CODEX 생성 파일] Backend A 계정 발급 및 상태 관리 작업을 위해 추가했습니다.
package com.aio.hospitalsafety.controller.admin;

import com.aio.hospitalsafety.config.HospitalUserDetails;
import com.aio.hospitalsafety.dto.admin.CreateUserRequest;
import com.aio.hospitalsafety.dto.admin.CreateUserResponse;
import com.aio.hospitalsafety.dto.admin.UserStatusResponse;
import com.aio.hospitalsafety.dto.admin.WardOptionResponse;
import com.aio.hospitalsafety.service.admin.UserProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class UserProvisioningController {
    private final UserProvisioningService userProvisioningService;

    public UserProvisioningController(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/users")
    public ResponseEntity<CreateUserResponse> createUser(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin,
            @Valid @RequestBody CreateUserRequest request) {
        HospitalUserDetails admin = requireAdmin(loginAdmin);
        CreateUserResponse response = userProvisioningService.createUser(admin.getHospitalId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @PatchMapping("/users/{userId}/disable")
    public UserStatusResponse disableUser(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin,
            @PathVariable String userId) {
        HospitalUserDetails admin = requireAdmin(loginAdmin);
        return userProvisioningService.disableUser(admin.getHospitalId(), userId);
    }

    @PatchMapping("/users/{userId}/reactivate")
    public UserStatusResponse reactivateUser(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin,
            @PathVariable String userId) {
        HospitalUserDetails admin = requireAdmin(loginAdmin);
        return userProvisioningService.reactivateUser(admin.getHospitalId(), userId);
    }

    @GetMapping("/wards")
    public List<WardOptionResponse> findWards(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin) {
        HospitalUserDetails admin = requireAdmin(loginAdmin);
        return userProvisioningService.findWards(admin.getHospitalId());
    }

    private HospitalUserDetails requireAdmin(HospitalUserDetails loginAdmin) {
        boolean isAdmin = loginAdmin != null && loginAdmin.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin) {
            throw new AccessDeniedException("병원 관리자만 사용할 수 있습니다.");
        }
        return loginAdmin;
    }
}
