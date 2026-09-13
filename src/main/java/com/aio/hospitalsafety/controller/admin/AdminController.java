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
        return "html/admin/admin";
    }

    @GetMapping({"/admin/admin_de", "/admin/admin_de/"})
    public String adminDeactivatedUsers() {
        // [09.13]수정내용: 비활성화 사용자 관리 화면의 기능명이 드러나는 HTML 경로로 연결합니다.
        return "html/admin/admin-inactive-users";
    }
}

