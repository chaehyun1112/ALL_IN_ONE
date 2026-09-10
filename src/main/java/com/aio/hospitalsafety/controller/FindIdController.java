// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.dto.FindIdRequestForm;
import com.aio.hospitalsafety.dto.FindIdSendCodeResponse;
import com.aio.hospitalsafety.dto.FindIdVerifyCodeResponse;
import com.aio.hospitalsafety.dto.FindIdVerifyForm;
import com.aio.hospitalsafety.service.FindIdService;
import com.aio.hospitalsafety.service.FindIdService.FindIdRequestResult;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.time.Instant;

/**
 * 로그아웃 상태에서 접근하는 아이디 찾기(이메일 인증) 화면을 처리한다.
 *
 * find-id.html 한 페이지 안에서 이름+이메일 입력 -> 인증코드 발송 -> 인증코드 확인까지
 * 전부 새로고침 없이 처리한다(회원가입 화면의 이메일 인증과 같은 방식). 발송/확인 두 요청은
 * 화면 전환이 아니라 JSON 응답을 돌려주는 AJAX 엔드포인트다.
 *
 * 인증코드와 발송 대상 이메일, 찾아낸 아이디는 DB가 아닌 HttpSession에만 잠시 보관하고
 * 검증이 끝나면(성공/만료/시도 초과 모두) 즉시 지운다. 비밀번호 재설정과 마찬가지로
 * 병원을 먼저 선택한 상태(AuthController.LOGIN_HOSPITAL_ID)에서만 접근할 수 있다.
 */
@Controller
public class FindIdController {

    private static final String FIND_ID_CODE = "FIND_ID_CODE";
    private static final String FIND_ID_USER_ID = "FIND_ID_USER_ID";
    private static final String FIND_ID_EXPIRES_AT = "FIND_ID_EXPIRES_AT";
    private static final String FIND_ID_ATTEMPTS = "FIND_ID_ATTEMPTS";

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final FindIdService findIdService;

    public FindIdController(FindIdService findIdService) {
        this.findIdService = findIdService;
    }

    /** 아이디 찾기 화면을 보여준다. 이 화면 하나로 발송/확인/결과까지 전부 처리한다. */
    @GetMapping("/id/find")
    public String showRequestForm(HttpSession session) {
        if (session.getAttribute(AuthController.LOGIN_HOSPITAL_ID) == null) {
            return "redirect:/login";
        }
        return "html/find-id";
    }

    /** 이름 + 이메일이 일치하면 인증코드를 발송한다(JSON 응답, 화면 전환 없음). */
    @PostMapping("/id/find/send-code")
    @ResponseBody
    public ResponseEntity<FindIdSendCodeResponse> sendCode(
            HttpSession session,
            @RequestBody @Valid FindIdRequestForm form,
            BindingResult bindingResult) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return ResponseEntity.status(401)
                    .body(new FindIdSendCodeResponse("UNAUTHORIZED", "다시 로그인해 주세요."));
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new FindIdSendCodeResponse("INVALID_INPUT", "이름과 이메일을 올바르게 입력해 주세요."));
        }

        String employeeName = form.getEmployeeName().trim();
        String email = form.getEmail().trim();
        FindIdRequestResult result = findIdService.requestVerificationCode(hospitalId, employeeName, email);

        if (result.status() == FindIdRequestResult.Status.NOT_FOUND) {
            return ResponseEntity.ok(
                    new FindIdSendCodeResponse("NOT_FOUND", "입력하신 정보와 일치하는 계정을 찾을 수 없습니다."));
        }
        if (result.status() == FindIdRequestResult.Status.SEND_FAILED) {
            return ResponseEntity.ok(
                    new FindIdSendCodeResponse("SEND_FAILED", "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요."));
        }

        session.setAttribute(FIND_ID_CODE, result.code());
        session.setAttribute(FIND_ID_USER_ID, result.userId());
        session.setAttribute(FIND_ID_EXPIRES_AT, Instant.now().plus(CODE_TTL).toEpochMilli());
        session.setAttribute(FIND_ID_ATTEMPTS, 0);

        return ResponseEntity.ok(new FindIdSendCodeResponse("SUCCESS", "인증코드를 보냈습니다."));
    }

    /** 인증코드를 확인하고, 일치하면 찾은 아이디를 함께 돌려준다(JSON 응답). */
    @PostMapping("/id/find/verify-code")
    @ResponseBody
    public ResponseEntity<FindIdVerifyCodeResponse> verifyCode(
            HttpSession session,
            @RequestBody @Valid FindIdVerifyForm form,
            BindingResult bindingResult) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return ResponseEntity.status(401)
                    .body(new FindIdVerifyCodeResponse("UNAUTHORIZED", "다시 로그인해 주세요.", null));
        }

        String storedCode = (String) session.getAttribute(FIND_ID_CODE);
        String storedUserId = (String) session.getAttribute(FIND_ID_USER_ID);
        Long expiresAt = (Long) session.getAttribute(FIND_ID_EXPIRES_AT);

        if (storedCode == null || storedUserId == null || expiresAt == null
                || Instant.now().toEpochMilli() > expiresAt) {
            clearFindIdSession(session);
            return ResponseEntity.ok(new FindIdVerifyCodeResponse(
                    "EXPIRED", "인증 시간이 만료되었습니다. 처음부터 다시 시도해 주세요.", null));
        }

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new FindIdVerifyCodeResponse("INVALID_INPUT", "인증코드는 숫자 6자리입니다.", null));
        }

        int attempts = (int) session.getAttribute(FIND_ID_ATTEMPTS);
        if (attempts >= MAX_ATTEMPTS) {
            clearFindIdSession(session);
            return ResponseEntity.ok(new FindIdVerifyCodeResponse(
                    "LOCKED", "인증 시도 횟수를 초과했습니다. 처음부터 다시 시도해 주세요.", null));
        }

        if (!storedCode.equals(form.getCode().trim())) {
            session.setAttribute(FIND_ID_ATTEMPTS, attempts + 1);
            return ResponseEntity.ok(new FindIdVerifyCodeResponse("INVALID", "인증코드가 일치하지 않습니다.", null));
        }

        String userId = storedUserId;
        clearFindIdSession(session);
        return ResponseEntity.ok(new FindIdVerifyCodeResponse("SUCCESS", "본인 확인이 완료되었습니다.", userId));
    }

    private void clearFindIdSession(HttpSession session) {
        session.removeAttribute(FIND_ID_CODE);
        session.removeAttribute(FIND_ID_USER_ID);
        session.removeAttribute(FIND_ID_EXPIRES_AT);
        session.removeAttribute(FIND_ID_ATTEMPTS);
    }
}
