package com.aio.hospitalsafety.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        // 관리자는 관리자 페이지로 이동
        if (admin) {
            return "redirect:/admin";
        }

        boolean approved = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("STATUS_APPROVED"));

        model.addAttribute("userId", authentication.getName());
        model.addAttribute("approved", approved);

        return "html/dashboard";
    }
}