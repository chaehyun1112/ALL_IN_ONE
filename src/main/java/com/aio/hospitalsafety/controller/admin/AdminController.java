package com.aio.hospitalsafety.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class AdminController {

    @GetMapping({"/admin", "/admin/"})
    public String adminHome(Authentication authentication, Model model) {
        String adminId = authentication == null ? "admin01" : authentication.getName();
        model.addAttribute("adminId", adminId);
        // [9.15] 추가내용: 기본 관리자 화면은 직원 관리 화면으로 표시한다.
        model.addAttribute("pageMode", "staff");
        return "html/admin/admin";
    }

    @GetMapping({"/admin/history", "/admin/history/"})
    public String adminHistory(Authentication authentication, Model model) {
        String adminId = authentication == null ? "admin01" : authentication.getName();
        model.addAttribute("adminId", adminId);
        // [9.15] 추가내용: 관리 이력은 직원 관리와 분리된 조회 전용 화면으로 표시한다.
        model.addAttribute("pageMode", "history");
        return "html/admin/admin";
    }

    @GetMapping({"/admin/admin_de", "/admin/admin_de/"})
    public String adminDeactivatedUsers() {
        return "html/admin/admin-inactive-users";
    }
}

