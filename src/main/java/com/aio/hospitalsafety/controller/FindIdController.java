package com.aio.hospitalsafety.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FindIdController {

    @GetMapping("/id/find")
    public String findIdPage() {
        return "html/auth/find-id";
    }
}