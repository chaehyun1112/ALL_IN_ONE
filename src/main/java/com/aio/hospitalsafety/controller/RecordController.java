package com.aio.hospitalsafety.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RecordController {

    @GetMapping("/Record")
    public String record() {
        return "html/auth/Record";
    }
}
