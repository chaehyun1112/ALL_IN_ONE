// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.dto.FindIdRequestForm;
import com.aio.hospitalsafety.dto.FindIdVerifyForm;
import com.aio.hospitalsafety.service.FindIdService;
import com.aio.hospitalsafety.service.FindIdService.FindIdRequestResult;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.Duration;
import java.time.Instant;

/**
 * 로그아웃 상태에서 접근하는 아이디 찾기(이메일 인증) 화면을 처리한다.
 *
 * 3단계 화면 흐름(find-id.html 하나를 model의 "step" 값으로 분기한다)
 * 1. request: 이름 + 이메일 입력 -> 일치하면 인증코드 메일 발송
 * 2. verify : 받은 인증코드 입력 -> 일치하면 결과 화면으로 이동
 * 3. result : 찾은 아이디 표시
 *
 * 인증코드와 발송 대상 이메일, 찾아낸 아이디는 DB가 아닌 HttpSession에만 잠시 보관하고
 * 검증이 끝나면(성공/만료/시도 초과 모두) 즉시 지운다. 비밀번호 재설정과 마찬가지로
 * 병원을 먼저 선택한 상태(AuthController.LOGIN_HOSPITAL_ID)에서만 접근할 수 있다.
 */
@Controller
public class FindIdController {

    private static final String FIND_ID_CODE = "FIND_ID_CODE";
    private static final String FIND_ID_USER_ID = "FIND_ID_USER_ID";
    private static final String FIND_ID_EMAIL = "FIND_ID_EMAIL";
    private static final String FIND_ID_EXPIRES_AT = "FIND_ID_EXPIRES_AT";
    private static final String FIND_ID_ATTEMPTS = "FIND_ID_ATTEMPTS";

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final FindIdService findIdService;

    public FindIdController(FindIdService findIdService) {
        this.findIdService = findIdService;
    }

    /** 아이디 찾기 1단계 화면을 보여준다. */
    @GetMapping("/id/find")
    public String showRequestForm(HttpSession session, Model model) {
        if (session.getAttribute(AuthController.LOGIN_HOSPITAL_ID) == null) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("findIdRequestForm")) {
            model.addAttribute("findIdRequestForm", new FindIdRequestForm());
        }
        model.addAttribute("step", "request");
        return "html/find-id";
    }

    /** 이름 + 이메일이 일치하면 인증코드를 발송하고 2단계 화면으로 이동한다. */
    @PostMapping("/id/find/send")
    public String sendVerificationCode(
            HttpSession session,
            @Valid @ModelAttribute("findIdRequestForm") FindIdRequestForm form,
            BindingResult bindingResult,
            Model model) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("step", "request");
            return "html/find-id";
        }

        String employeeName = form.getEmployeeName().trim();
        String email = form.getEmail().trim();
        FindIdRequestResult result = findIdService.requestVerificationCode(hospitalId, employeeName, email);

        if (result.status() == FindIdRequestResult.Status.NOT_FOUND) {
            model.addAttribute("requestError", "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
            model.addAttribute("step", "request");
            return "html/find-id";
        }
        if (result.status() == FindIdRequestResult.Status.SEND_FAILED) {
            model.addAttribute("requestError", "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.");
            model.addAttribute("step", "request");
            return "html/find-id";
        }

        session.setAttribute(FIND_ID_CODE, result.code());
        session.setAttribute(FIND_ID_USER_ID, result.userId());
        session.setAttribute(FIND_ID_EMAIL, email);
        session.setAttribute(FIND_ID_EXPIRES_AT, Instant.now().plus(CODE_TTL).toEpochMilli());
        session.setAttribute(FIND_ID_ATTEMPTS, 0);

        model.addAttribute("step", "verify");
        model.addAttribute("maskedEmail", maskEmail(email));
        model.addAttribute("findIdVerifyForm", new FindIdVerifyForm());
        return "html/find-id";
    }

    /** 인증코드를 확인하고, 일치하면 찾은 아이디를 보여준다. */
    @PostMapping("/id/find/verify")
    public String verifyCode(
            HttpSession session,
            @Valid @ModelAttribute("findIdVerifyForm") FindIdVerifyForm form,
            BindingResult bindingResult,
            Model model) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return "redirect:/login";
        }

        String storedCode = (String) session.getAttribute(FIND_ID_CODE);
        String storedUserId = (String) session.getAttribute(FIND_ID_USER_ID);
        String storedEmail = (String) session.getAttribute(FIND_ID_EMAIL);
        Long expiresAt = (Long) session.getAttribute(FIND_ID_EXPIRES_AT);

        // 인증 요청 자체가 없거나(직접 URL 접근) 유효 시간이 지났으면 처음부터 다시 시작한다.
        if (storedCode == null || storedUserId == null || expiresAt == null
                || Instant.now().toEpochMilli() > expiresAt) {
            clearFindIdSession(session);
            model.addAttribute("requestError", "인증 시간이 만료되었습니다. 처음부터 다시 시도해 주세요.");
            model.addAttribute("step", "request");
            model.addAttribute("findIdRequestForm", new FindIdRequestForm());
            return "html/find-id";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("step", "verify");
            model.addAttribute("maskedEmail", maskEmail(storedEmail));
            return "html/find-id";
        }

        int attempts = (int) session.getAttribute(FIND_ID_ATTEMPTS);
        if (attempts >= MAX_ATTEMPTS) {
            clearFindIdSession(session);
            model.addAttribute("requestError", "인증 시도 횟수를 초과했습니다. 처음부터 다시 시도해 주세요.");
            model.addAttribute("step", "request");
            model.addAttribute("findIdRequestForm", new FindIdRequestForm());
            return "html/find-id";
        }

        if (!storedCode.equals(form.getCode().trim())) {
            session.setAttribute(FIND_ID_ATTEMPTS, attempts + 1);
            model.addAttribute("verifyError", "인증코드가 일치하지 않습니다.");
            model.addAttribute("step", "verify");
            model.addAttribute("maskedEmail", maskEmail(storedEmail));
            return "html/find-id";
        }

        clearFindIdSession(session);
        model.addAttribute("step", "result");
        model.addAttribute("foundUserId", storedUserId);
        return "html/find-id";
    }

    private void clearFindIdSession(HttpSession session) {
        session.removeAttribute(FIND_ID_CODE);
        session.removeAttribute(FIND_ID_USER_ID);
        session.removeAttribute(FIND_ID_EMAIL);
        session.removeAttribute(FIND_ID_EXPIRES_AT);
        session.removeAttribute(FIND_ID_ATTEMPTS);
    }

    /** 화면에 "ab***@domain.com" 형태로 이메일 일부만 보여준다. */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
