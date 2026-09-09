package com.aio.hospitalsafety.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.dto.UserDto;
import com.aio.hospitalsafety.service.AdminService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping({"/admin", "/admin/"})
    public String adminHome(HttpSession session) {
        if (findHospitalDomain(session) == null) {
            return "redirect:/";
        }
        return "html/admin/admin";
    }

    @GetMapping("/api/admin/users")
    @ResponseBody
    public ResponseEntity<List<UserDto>> findUsers(HttpSession session) {
        String hospitalDomain = findHospitalDomain(session);
        if (hospitalDomain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminService.findUsers(hospitalDomain));
    }

    @PatchMapping("/api/admin/users/{userId}/approve")
    @ResponseBody
    public ResponseEntity<Void> approveUser(
            @PathVariable String userId,
            HttpSession session
    ) {
        String hospitalDomain = findHospitalDomain(session);
        if (hospitalDomain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return adminService.approveUser(hospitalDomain, userId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @DeleteMapping("/api/admin/users/{userId}/reject")
    @ResponseBody
    public ResponseEntity<Void> rejectUser(
            @PathVariable String userId,
            HttpSession session
    ) {
        String hospitalDomain = findHospitalDomain(session);
        if (hospitalDomain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return adminService.rejectUser(hospitalDomain, userId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    private String findHospitalDomain(HttpSession session) {
        Object hospitalDomain = session.getAttribute(SessionConstants.HOSPITAL_DOMAIN);
        if (!(hospitalDomain instanceof String value) || value.isBlank()) {
            return null;
        }
        return value;
    }
}
