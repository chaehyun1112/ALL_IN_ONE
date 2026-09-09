// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.dto.HospitalLoginForm;
import com.aio.hospitalsafety.service.HospitalService;
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

    /**
     * 로그인 1단계인 병원 구분 ID 입력 화면을 보여준다.
     *
     * 예전에는 여기서 Session의 LOGIN_HOSPITAL_ID/NAME을 무조건 지웠다("병원 변경 시
     * 이전 선택값 제거" 의도). 하지만 GET 요청에 상태를 바꾸는 부작용을 넣으면 안 된다 —
     * user-login.html의 "다른 병원 선택" 링크(href="/login")가 화면에 보이기만 해도
     * 브라우저의 링크 프리페치(prefetch) 기능이 사용자가 클릭하지 않았는데도 조용히
     * GET /login을 미리 요청해서, 방금 선택한 병원 정보가 세션에서 사라지는 버그가
     * 있었다(비밀번호 재설정 링크가 로그인 화면으로 튕기는 문제로 발견됨).
     * 병원을 바꾸는 실제 반영은 POST /login/hospital이 새 값으로 덮어쓰는 것만으로
     * 충분하고, 이 화면 자체는 세션 값을 표시하지 않으므로 GET에서 지울 이유가 없다.
     */
    @GetMapping("/login")
    public String hospitalLogin(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("hospitalLoginForm", new HospitalLoginForm());
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
    public String userLogin(Authentication authentication, HttpSession session, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }

        String hospitalId = (String) session.getAttribute(LOGIN_HOSPITAL_ID);
        String hospitalName = (String) session.getAttribute(LOGIN_HOSPITAL_NAME);
        if (hospitalId == null || hospitalName == null) {
            return "redirect:/login";
        }

        model.addAttribute("hospitalId", hospitalId);
        model.addAttribute("hospitalName", hospitalName);
        return "html/user-login";
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
