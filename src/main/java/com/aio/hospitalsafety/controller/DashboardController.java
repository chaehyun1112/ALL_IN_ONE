package com.aio.hospitalsafety.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    // [수정완료] 대시보드와 설정 화면에 현재 로그인에서 입력한 이름을 전달합니다.
    @org.springframework.web.bind.annotation.ModelAttribute("displayName")
    public String displayName(jakarta.servlet.http.HttpSession session) {
        Object name = session.getAttribute(com.aio.hospitalsafety.config.UserDisplaySession.DISPLAY_NAME);
        return name instanceof String && !((String) name).isBlank() ? (String) name : "이름 미입력";
    }

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

    // [수정완료] 대시보드 메뉴에서 설정 HTML 화면으로 연결합니다.
    @GetMapping("/settings")
    public String settings(Authentication authentication, @RequestParam(defaultValue = "all") String filter, Model model) {
        model.addAttribute("filter", filter);
        model.addAttribute("userId", authentication == null ? "사용자" : authentication.getName());
        return "html/settings";
    }
}
