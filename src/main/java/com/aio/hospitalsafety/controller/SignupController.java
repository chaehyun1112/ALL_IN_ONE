package com.aio.hospitalsafety.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.dto.Signup;
import com.aio.hospitalsafety.service.SignupService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    // 회원가입 화면
    @GetMapping("/signup")
    public String showSignupPage(
            HttpSession session,
            Model model
    ) {
        String hospitalDomain =
                (String) session.getAttribute(
                        SessionConstants.HOSPITAL_DOMAIN
                );

        // 병원 선택 과정 없이 접근한 경우
        if (hospitalDomain == null || hospitalDomain.isBlank()) {
            return "redirect:/";
        }

        model.addAttribute(
                "signup",
                new Signup(null, null, null, null, null)
        );

        model.addAttribute(
                "wards",
                signupService.getWardsByHospital(hospitalDomain)
        );

        return "html/auth/signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signupUser(
            @Valid @ModelAttribute("signup") Signup signup,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String hospitalDomain =
                (String) session.getAttribute(
                        SessionConstants.HOSPITAL_DOMAIN
                );

        if (hospitalDomain == null || hospitalDomain.isBlank()) {
            return "redirect:/";
        }

        // DTO 입력값 검증 실패
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "wards",
                    signupService.getWardsByHospital(hospitalDomain)
            );

            return "html/auth/signup";
        }

        try {
            signupService.signupUser(
                    signup,
                    hospitalDomain
            );
        } catch (IllegalArgumentException exception) {
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            model.addAttribute(
                    "wards",
                    signupService.getWardsByHospital(hospitalDomain)
            );

            return "html/auth/signup";
        }

        redirectAttributes.addFlashAttribute(
                "signupSuccessMessage",
                "회원가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다."
        );

        return "redirect:/login";
    }

    // 사용자 아이디 중복확인
    @GetMapping("/api/users/check-user-id")
    @ResponseBody
    public Map<String, Boolean> checkUserId(
            @RequestParam("userId") String userId
    ) {
        boolean available =
                signupService.isUserIdAvailable(userId);

        return Map.of("available", available);
    }
}