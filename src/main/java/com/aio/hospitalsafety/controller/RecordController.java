package com.aio.hospitalsafety.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RecordController {

    @GetMapping("/Record")
    public String record() {
        // [09.13]수정내용: 조치 기록 화면 이동에 맞춰 새 HTML 경로를 반환합니다.
        return "html/record/record";
    }
}




