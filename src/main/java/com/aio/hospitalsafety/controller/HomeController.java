package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.dto.HospitalDto;
import com.aio.hospitalsafety.service.HospitalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 병합 메모(park + chae): GET /login은 AuthController(park)가 소유한다.
 * 원래 chae 브랜치에는 이 Controller에도 GET /login(1단계 로그인 화면)이 있었지만,
 * park의 2단계 로그인(AuthController)이 실제 인증까지 연결된 유일한 구현이라 그쪽으로 통일했다.
 * 이 Controller는 회원가입 진입 전 "병원 도메인 선택"(/, /domain)만 담당한다.
 */
@Controller
public class HomeController {

    private final HospitalService hospitalService;

    public HomeController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping("/")
    public String home() {
        return "html/index";
    }

    @PostMapping("/domain")
    public String selectHospital(@RequestParam(defaultValue = "") String hospitalDomain,
                                 HttpSession session, Model model) {
        session.removeAttribute(SessionConstants.HOSPITAL_DOMAIN);
        model.addAttribute("hospitalDomain", hospitalDomain);
        if (hospitalDomain.isBlank()) {
            model.addAttribute("domainError", "병원 도메인을 입력해 주세요.");
            return "html/index";
        }
        try {
            HospitalDto hospital = hospitalService.findHospitalByDomain(hospitalDomain);
            if (hospital == null) {
                model.addAttribute("domainError", "등록되지 않은 병원 도메인입니다. 다시 확인해 주세요.");
                return "html/index";
            }
            session.setAttribute(SessionConstants.HOSPITAL_DOMAIN, hospital.hospitalDomain());
            return "redirect:/access-type";
        } catch (DataAccessException exception) {
            model.addAttribute("domainError", "병원 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return "html/index";
        }
    }
}
