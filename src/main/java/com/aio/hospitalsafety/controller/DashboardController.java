package com.aio.hospitalsafety.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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
    public String dashboard(Authentication authentication, Model model, HttpServletResponse response) {

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        // 관리자는 관리자 페이지로 이동
        if (admin) {
            return "redirect:/admin";
        }

        response.setHeader("Cache-Control", "no-store");

        boolean approved = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("STATUS_APPROVED"));

        model.addAttribute("userId", authentication.getName());
        model.addAttribute("approved", approved);

        // [09.13]수정내용: 이동한 대시보드 HTML 경로를 반환하도록 경로를 갱신했습니다.
        return "html/dashboard/dashboard";
    }

    // 대시보드를 열어 둔 동안의 요청으로 기존 인증 세션의 유휴 시간을 갱신합니다.
    @GetMapping("/api/dashboard/session")
    public ResponseEntity<Void> keepDashboardSession() {
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    // [수정완료] 대시보드 메뉴에서 설정 HTML 화면으로 연결합니다.
    @GetMapping("/settings")
    public String settings(Authentication authentication, @RequestParam(defaultValue = "all") String filter, Model model) {
        model.addAttribute("filter", filter);
        model.addAttribute("userId", authentication == null ? "사용자" : authentication.getName());
        // [09.13]수정내용: 설정 화면을 settings 폴더의 화면 파일로 연결합니다.
        return "html/settings/settings";
    }
}
