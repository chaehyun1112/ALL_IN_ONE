package com.aio.hospitalsafety.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessTypeController {

    @GetMapping("/access-type")
    public String accessType() {
        return "html/access-type";
    }
}
