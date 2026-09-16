package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.config.UserDisplaySession;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 이 화면은 메인 대시보드(DashboardController)를 그대로 복사한 화면에
 * 순찰 로봇 마커만 추가한 것이다. 그래서 dashboard.html이 쓰는 모델 값을
 * 여기서도 똑같이 채워준다.
 */
@Controller
public class RobotController {

    @ModelAttribute("displayName")
    public String displayName(HttpSession session) {
        Object name = session.getAttribute(UserDisplaySession.DISPLAY_NAME);
        return name instanceof String && !((String) name).isBlank() ? (String) name : "이름 미입력";
    }

    @GetMapping("/robot")
    public String robot(Authentication authentication, Model model) {
        boolean approved = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("STATUS_APPROVED"));

        model.addAttribute("userId", authentication.getName());
        model.addAttribute("approved", approved);

        return "html/robot/robot";
    }
}
