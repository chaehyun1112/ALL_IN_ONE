package com.aio.hospitalsafety.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping({"/admin", "/admin/"})
    public String adminHome() {
        return "html/admin/admin";
    }

    @GetMapping({"/admin/admin_de", "/admin/admin_de/"})
    public String adminDeactivatedUsers() {
        return "html/admin/admin_de";
    }
}
