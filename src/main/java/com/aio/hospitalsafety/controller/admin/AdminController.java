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
        return "html/admin/admin_de";
    }
}
