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

@Controller
public class HomeController {

    private final HospitalService hospitalService;

    public HomeController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping("/")
    public String home() {
        return "html/auth/index";
    }

    // [09.13]추가내용: Figma의 접속 유형 선택 화면을 /role 경로로 제공한다.
    @GetMapping("/role")
    public String roleSelection(HttpSession session) {
        // [09.13]수정내용: 병원 도메인을 먼저 선택한 경우에만 접속 유형 화면을 표시한다.
        if (session.getAttribute(SessionConstants.HOSPITAL_DOMAIN) == null) {
            return "redirect:/";
        }
        return "html/auth/role";
    }

    @PostMapping("/domain")
    public String selectHospital(@RequestParam(defaultValue = "") String hospitalDomain,
                                 HttpSession session, Model model) {
        session.removeAttribute(SessionConstants.HOSPITAL_DOMAIN);
        model.addAttribute("hospitalDomain", hospitalDomain);
        if (hospitalDomain.isBlank()) {
            model.addAttribute("domainError", "병원 도메인을 입력해 주세요.");
            return "html/auth/index";
        }
        try {
            HospitalDto hospital = hospitalService.findHospitalByDomain(hospitalDomain);
            if (hospital == null) {
                model.addAttribute("domainError", "등록되지 않은 병원 도메인입니다. 다시 확인해 주세요.");
                return "html/auth/index";
            }
            session.setAttribute(SessionConstants.HOSPITAL_DOMAIN, hospital.hospitalDomain());
            // [09.13]수정내용: 도메인 확인 후 로그인 전에 관리자·간호사 접속 유형을 선택하도록 연결한다.
            return "redirect:/role";
        } catch (DataAccessException exception) {
            model.addAttribute("domainError", "병원 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return "html/auth/index";
        }
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String role,
                        HttpSession session, Model model) {
        // [09.13]수정내용: 접속 유형 없이 로그인 주소에 직접 접근하면 유형 선택 화면을 먼저 표시한다.
        if (role == null || role.isBlank()) {
            return "redirect:/role";
        }
        String hospitalDomain = (String) session.getAttribute(SessionConstants.HOSPITAL_DOMAIN);
        try {
            HospitalDto hospital = hospitalService.findHospitalByDomain(hospitalDomain);
            if (hospital != null) {
                model.addAttribute("hospitalName", hospital.hospitalName());
                model.addAttribute("hospitalId", hospital.hospitalDomain());
                // [09.13]추가내용: 접속 유형을 로그인 화면에 전달하여 관리자와 간호사 입력 항목을 구분한다.
                model.addAttribute("loginRole", "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER");
                return "html/auth/login";
            }
        } catch (DataAccessException exception) {
            model.addAttribute("domainError", "병원 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return "html/auth/index";
        }
        session.removeAttribute(SessionConstants.HOSPITAL_DOMAIN);
        return "redirect:/";
    }
}
