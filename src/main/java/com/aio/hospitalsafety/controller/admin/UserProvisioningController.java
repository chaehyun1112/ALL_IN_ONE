package com.aio.hospitalsafety.controller.admin;

import com.aio.hospitalsafety.config.HospitalUserDetails;
import com.aio.hospitalsafety.dto.admin.CreateUserRequest;
import com.aio.hospitalsafety.dto.admin.CreateUserResponse;
import com.aio.hospitalsafety.service.admin.UserProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class UserProvisioningController {

    private final UserProvisioningService userProvisioningService;

    public UserProvisioningController(
            UserProvisioningService userProvisioningService
    ) {
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/users")
    public ResponseEntity<CreateUserResponse> createUser(
            @AuthenticationPrincipal HospitalUserDetails loginAdmin,
            @Valid @RequestBody CreateUserRequest request
    ) {
        HospitalUserDetails admin = requireAdmin(loginAdmin);

        CreateUserResponse response =
                userProvisioningService.createUser(
                        admin.getHospitalId(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    private HospitalUserDetails requireAdmin(
            HospitalUserDetails loginAdmin
    ) {
        boolean isAdmin =
                loginAdmin != null
                && loginAdmin.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                );

        if (!isAdmin) {
            throw new AccessDeniedException(
                    "병원 관리자만 사용할 수 있습니다."
            );
        }

        return loginAdmin;
    }
}