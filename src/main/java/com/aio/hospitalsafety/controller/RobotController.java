package com.aio.hospitalsafety.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RobotController {

    @GetMapping("/robot")
    public String robot() {
        return "html/robot";
    }
}
