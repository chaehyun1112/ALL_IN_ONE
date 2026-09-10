// PGH
package com.aio.hospitalsafety.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그아웃 상태에서 접근하는 아이디 찾기 화면을 보여준다.
 *
 * 이메일 인증(발송/확인) 백엔드는 팀원이 별도로 구현 중이라 이 Controller에서는
 * 화면 렌더링만 담당한다. 병원을 먼저 선택한 상태(AuthController.LOGIN_HOSPITAL_ID)에서만
 * 접근할 수 있다.
 */
@Controller
public class FindIdController {

    @GetMapping("/id/find")
    public String showRequestForm(HttpSession session) {
        if (session.getAttribute(AuthController.LOGIN_HOSPITAL_ID) == null) {
            return "redirect:/login";
        }
        return "html/find-id";
    }
}
