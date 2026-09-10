// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.common.AuthenticationSessionManager;
import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.dto.HospitalLoginForm;
import com.aio.hospitalsafety.service.HospitalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

/**
 * Dooray와 같은 2단계 로그인 화면 흐름을 담당한다.
 *
 * 1단계: 병원 구분 ID 입력 -> 실제 TB_HOSPITAL 등록 여부 확인
 * 2단계: 해당 병원 화면에서 직원 ID/PW 입력 -> Spring Security 인증
 *
 * PW 비교 자체는 이 Controller가 하지 않는다. POST /login/user 요청은
 * SecurityConfig의 로그인 필터가 가로채 처리한다.
 */
@Controller
public class AuthController {

    // 다른 Controller에서도 같은 이름으로 Session 값을 읽도록 상수로 관리한다.
    public static final String LOGIN_HOSPITAL_ID = "LOGIN_HOSPITAL_ID";
    public static final String LOGIN_HOSPITAL_NAME = "LOGIN_HOSPITAL_NAME";

    private final HospitalService hospitalService;

    public AuthController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    /** 로그인 1단계인 병원 구분 ID 입력 화면을 보여준다. */
    @GetMapping("/login")
    public String hospitalLogin(Authentication authentication,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) {
        HttpSession session = AuthenticationSessionManager.expireAuthentication(
                request, response, authentication);

        String hospitalId = (String) session.getAttribute(SessionConstants.HOSPITAL_DOMAIN);
        if (hospitalId == null || hospitalId.isBlank()) {
            return "redirect:/";
        }

        Optional<Hospital> hospital = hospitalService.findRegisteredHospital(hospitalId);
        if (hospital.isEmpty()) {
            session.removeAttribute(SessionConstants.HOSPITAL_DOMAIN);
            return "redirect:/";
        }

        session.setAttribute(LOGIN_HOSPITAL_ID, hospital.get().hospitalId());
        session.setAttribute(LOGIN_HOSPITAL_NAME, hospital.get().hospitalName());
        model.addAttribute("hospitalId", hospital.get().hospitalId());
        model.addAttribute("hospitalName", hospital.get().hospitalName());
        return "html/login";
    }

    /**
     * 로그인 1단계 입력을 처리한다.
     * DB에 등록된 병원일 때만 병원 정보를 Session에 저장하고 2단계로 이동한다.
     */
    @PostMapping("/login/hospital")
    public String selectHospital(
            @Valid @ModelAttribute("hospitalLoginForm") HospitalLoginForm form,
            BindingResult bindingResult,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "html/login";
        }

        Optional<Hospital> hospital = hospitalService.findRegisteredHospital(form.getHospitalId());
        if (hospital.isEmpty()) {
            bindingResult.rejectValue("hospitalId", "notFound", "등록되지 않은 병원 구분 ID입니다.");
            return "html/login";
        }

        session.setAttribute(LOGIN_HOSPITAL_ID, hospital.get().hospitalId());
        session.setAttribute(LOGIN_HOSPITAL_NAME, hospital.get().hospitalName());
        return "redirect:/login/user";
    }

    /** 로그인 2단계인 직원 ID/PW 입력 화면을 보여준다. */
    @GetMapping("/login/user")
    public String userLogin(Authentication authentication,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            AuthenticationSessionManager.expireAuthentication(request, response, authentication);
            return "redirect:/login";
        }

        HttpSession session = request.getSession();

        String hospitalId = (String) session.getAttribute(LOGIN_HOSPITAL_ID);
        String hospitalName = (String) session.getAttribute(LOGIN_HOSPITAL_NAME);
        if (hospitalId == null || hospitalName == null) {
            return "redirect:/login";
        }

        model.addAttribute("hospitalId", hospitalId);
        model.addAttribute("hospitalName", hospitalName);
        return "html/login";
    }

    /**
     * 로그인 성공 후 보여줄 임시 화면이다.
     * PENDING 사용자는 로그인은 되지만 승인 대기 안내만 확인할 수 있다.
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        boolean approved = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("STATUS_APPROVED"));

        model.addAttribute("userId", authentication.getName());
        model.addAttribute("approved", approved);
        return "html/dashboard";
    }
}
