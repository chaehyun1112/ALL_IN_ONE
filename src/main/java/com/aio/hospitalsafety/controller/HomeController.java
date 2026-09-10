package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.service.HospitalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

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
            Optional<Hospital> hospital = hospitalService.findRegisteredHospital(hospitalDomain);
            if (hospital.isEmpty()) {
                model.addAttribute("domainError", "등록되지 않은 병원 도메인입니다. 다시 확인해 주세요.");
                return "html/index";
            }
            session.setAttribute(SessionConstants.HOSPITAL_DOMAIN, hospital.get().hospitalId());
            return "redirect:/login";
        } catch (DataAccessException exception) {
            model.addAttribute("domainError", "병원 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return "html/index";
        }
    }

}
