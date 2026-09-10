// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.dto.CheckEmployeeIdForm;
import com.aio.hospitalsafety.dto.CheckEmployeeIdResponse;
import com.aio.hospitalsafety.dto.NewPasswordForm;
import com.aio.hospitalsafety.dto.PasswordChangeForm;
import com.aio.hospitalsafety.dto.PasswordResetCodeForm;
import com.aio.hospitalsafety.dto.PasswordResetForm;
import com.aio.hospitalsafety.dto.PasswordResetSendCodeResponse;
import com.aio.hospitalsafety.dto.PasswordResetVerifyCodeResponse;
import com.aio.hospitalsafety.config.HospitalUserDetails;
import com.aio.hospitalsafety.service.PasswordResetService;
import com.aio.hospitalsafety.service.PasswordResetService.IdentifyResult;
import com.aio.hospitalsafety.service.UserService;
import com.aio.hospitalsafety.service.UserService.PasswordChangeResult;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.time.Instant;

/**
 * 비밀번호 관련 화면 요청을 처리하는 MVC Controller다.
 *
 * 이 클래스가 담당하는 기능은 두 가지다.
 * 1. 비로그인 사용자를 위한 비밀번호 재설정(본인 확인 -> 이메일 인증코드 -> 새 비밀번호).
 *    한 화면(password-reset.html) 안에서 발송/확인은 새로고침 없는 AJAX로 처리하고,
 *    새 비밀번호 저장만 일반 form POST로 처리한다(회원가입 화면의 이메일 인증과 같은 방식).
 * 2. 로그인한 사용자의 현재 PW 확인 후 새 PW 변경
 *
 * Controller는 HTTP 요청값 검사와 화면 이동, Session 상태 전이를 담당하고,
 * 실제 사용자 조회·이메일 발송·비밀번호 저장은 Service 계층에 위임한다.
 */
@Controller
public class UserPasswordController {

    private static final String PWRESET_CODE = "PWRESET_CODE";
    private static final String PWRESET_USER_ID = "PWRESET_USER_ID";
    private static final String PWRESET_EXPIRES_AT = "PWRESET_EXPIRES_AT";
    private static final String PWRESET_ATTEMPTS = "PWRESET_ATTEMPTS";
    private static final String PWRESET_VERIFIED_USER_ID = "PWRESET_VERIFIED_USER_ID";

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    // final 필드는 객체 생성 후 다른 Service로 바뀌지 않는다.
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    /**
     * 생성자 주입 방식이다. Spring이 Bean을 찾아 자동으로 전달한다.
     */
    public UserPasswordController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * 로그아웃 상태에서 접근하는 비밀번호 재설정 화면이다.
     *
     * 본인 확인 수단: 직원 ID + 이름 + 이메일이 모두 일치해야 한다. 로그인 1단계
     * (AuthController)에서 Session에 저장해 둔 병원 구분 ID를 그대로 사용하므로,
     * 병원을 먼저 선택한 상태에서만 접근할 수 있다. Session에 이미 이메일 인증을 마친
     * 직원 ID가 남아 있으면(예: 새 비밀번호 검증 실패 후 재렌더링) 새 비밀번호 입력
     * 영역을 곧바로 보여준다.
     */
    @GetMapping("/password/reset")
    public String resetGuide(HttpSession session, Model model) {
        if (session.getAttribute(AuthController.LOGIN_HOSPITAL_ID) == null) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("newPasswordForm")) {
            model.addAttribute("newPasswordForm", new NewPasswordForm());
        }
        model.addAttribute("verified", session.getAttribute(PWRESET_VERIFIED_USER_ID) != null);
        return "html/password-reset";
    }

    /**
     * '아이디 확인' 버튼 요청이다(JSON 응답, 화면 전환 없음). 아이디만으로 존재 여부를
     * 알려주지 않고, 아이디+이름이 함께 일치할 때만 확인된 것으로 응답한다.
     */
    @PostMapping("/password/reset/check-id")
    @ResponseBody
    public ResponseEntity<CheckEmployeeIdResponse> checkEmployeeId(
            HttpSession session,
            @RequestBody @Valid CheckEmployeeIdForm form,
            BindingResult bindingResult) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return ResponseEntity.status(401)
                    .body(new CheckEmployeeIdResponse("UNAUTHORIZED", "다시 로그인해 주세요."));
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new CheckEmployeeIdResponse("INVALID_INPUT", "아이디와 이름을 올바르게 입력해 주세요."));
        }

        boolean matches = passwordResetService.employeeIdMatchesName(
                hospitalId, form.getEmployeeId().trim(), form.getEmployeeName().trim());

        if (matches) {
            return ResponseEntity.ok(new CheckEmployeeIdResponse("MATCH", "확인된 아이디입니다."));
        }
        return ResponseEntity.ok(new CheckEmployeeIdResponse("NOT_MATCH", "아이디와 이름이 일치하는 계정을 찾을 수 없습니다."));
    }

    /** 직원 ID+이름+이메일이 일치하면 인증코드를 발송한다(JSON 응답, 화면 전환 없음). */
    @PostMapping("/password/reset/verify-identity")
    @ResponseBody
    public ResponseEntity<PasswordResetSendCodeResponse> verifyIdentity(
            HttpSession session,
            @RequestBody @Valid PasswordResetForm form,
            BindingResult bindingResult) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return ResponseEntity.status(401)
                    .body(new PasswordResetSendCodeResponse("UNAUTHORIZED", "다시 로그인해 주세요."));
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new PasswordResetSendCodeResponse("INVALID_INPUT", "아이디, 이름, 이메일을 올바르게 입력해 주세요."));
        }

        String employeeId = form.getEmployeeId().trim();
        String employeeName = form.getEmployeeName().trim();
        String email = form.getEmail().trim();
        IdentifyResult result = passwordResetService.verifyIdentityAndSendCode(
                hospitalId, employeeId, employeeName, email);

        if (result.status() == IdentifyResult.Status.NOT_FOUND) {
            return ResponseEntity.ok(
                    new PasswordResetSendCodeResponse("NOT_FOUND", "입력하신 정보와 일치하는 계정을 찾을 수 없습니다."));
        }
        if (result.status() == IdentifyResult.Status.SEND_FAILED) {
            return ResponseEntity.ok(
                    new PasswordResetSendCodeResponse("SEND_FAILED", "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요."));
        }

        session.setAttribute(PWRESET_CODE, result.code());
        session.setAttribute(PWRESET_USER_ID, result.userId());
        session.setAttribute(PWRESET_EXPIRES_AT, Instant.now().plus(CODE_TTL).toEpochMilli());
        session.setAttribute(PWRESET_ATTEMPTS, 0);

        return ResponseEntity.ok(new PasswordResetSendCodeResponse("SUCCESS", "인증코드를 보냈습니다."));
    }

    /** 인증코드를 확인한다(JSON 응답). 성공하면 새 비밀번호 저장 단계를 진행할 수 있는 상태가 된다. */
    @PostMapping("/password/reset/verify-code")
    @ResponseBody
    public ResponseEntity<PasswordResetVerifyCodeResponse> verifyCode(
            HttpSession session,
            @RequestBody @Valid PasswordResetCodeForm form,
            BindingResult bindingResult) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return ResponseEntity.status(401)
                    .body(new PasswordResetVerifyCodeResponse("UNAUTHORIZED", "다시 로그인해 주세요."));
        }

        String storedCode = (String) session.getAttribute(PWRESET_CODE);
        String storedUserId = (String) session.getAttribute(PWRESET_USER_ID);
        Long expiresAt = (Long) session.getAttribute(PWRESET_EXPIRES_AT);

        if (storedCode == null || storedUserId == null || expiresAt == null
                || Instant.now().toEpochMilli() > expiresAt) {
            clearPasswordResetSession(session);
            return ResponseEntity.ok(new PasswordResetVerifyCodeResponse(
                    "EXPIRED", "인증 시간이 만료되었습니다. 처음부터 다시 시도해 주세요."));
        }

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new PasswordResetVerifyCodeResponse("INVALID_INPUT", "인증코드는 숫자 6자리입니다."));
        }

        int attempts = (int) session.getAttribute(PWRESET_ATTEMPTS);
        if (attempts >= MAX_ATTEMPTS) {
            clearPasswordResetSession(session);
            return ResponseEntity.ok(new PasswordResetVerifyCodeResponse(
                    "LOCKED", "인증 시도 횟수를 초과했습니다. 처음부터 다시 시도해 주세요."));
        }

        if (!storedCode.equals(form.getCode().trim())) {
            session.setAttribute(PWRESET_ATTEMPTS, attempts + 1);
            return ResponseEntity.ok(new PasswordResetVerifyCodeResponse("INVALID", "인증코드가 일치하지 않습니다."));
        }

        // 코드 검증까지 끝났으므로 코드 관련 세션 값은 지우고, 확인된 직원 ID만 남긴다.
        session.removeAttribute(PWRESET_CODE);
        session.removeAttribute(PWRESET_EXPIRES_AT);
        session.removeAttribute(PWRESET_ATTEMPTS);
        session.setAttribute(PWRESET_VERIFIED_USER_ID, storedUserId);

        return ResponseEntity.ok(new PasswordResetVerifyCodeResponse("SUCCESS", "본인 확인이 완료되었습니다."));
    }

    /** 이메일 인증까지 끝난 상태에서만 실제로 새 비밀번호를 저장한다. */
    @PostMapping("/password/reset")
    public String resetPassword(
            HttpSession session,
            @Valid @ModelAttribute("newPasswordForm") NewPasswordForm form,
            BindingResult bindingResult,
            Model model) {
        String hospitalId = (String) session.getAttribute(AuthController.LOGIN_HOSPITAL_ID);
        if (hospitalId == null) {
            return "redirect:/login";
        }

        String verifiedUserId = (String) session.getAttribute(PWRESET_VERIFIED_USER_ID);
        if (verifiedUserId == null) {
            // 이메일 인증 없이 곧바로 이 URL에 접근한 경우 처음부터 다시 시작한다.
            model.addAttribute("formError", "이메일 인증을 먼저 완료해 주세요.");
            model.addAttribute("verified", false);
            return "html/password-reset";
        }

        if (bindingResult.hasErrors()) {
            clearPasswordFields(form);
            model.addAttribute("verified", true);
            return "html/password-reset";
        }

        passwordResetService.resetPassword(verifiedUserId, form.getNewPassword());
        session.removeAttribute(PWRESET_VERIFIED_USER_ID);
        return "redirect:/login?passwordChanged";
    }

    private void clearPasswordResetSession(HttpSession session) {
        session.removeAttribute(PWRESET_CODE);
        session.removeAttribute(PWRESET_USER_ID);
        session.removeAttribute(PWRESET_EXPIRES_AT);
        session.removeAttribute(PWRESET_ATTEMPTS);
        session.removeAttribute(PWRESET_VERIFIED_USER_ID);
    }

    /** 재설정 폼의 비밀번호 관련 입력값을 지워 화면 재렌더링 시 원문이 남지 않게 한다. */
    private void clearPasswordFields(NewPasswordForm form) {
        form.setNewPassword(null);
        form.setPasswordConfirm(null);
    }

    /**
     * GET /user/password 요청으로 비밀번호 변경 폼을 보여준다.
     * SecurityConfig의 anyRequest().authenticated() 규칙 때문에 로그인 사용자만 접근할 수 있다.
     *
     * @param model PasswordChangeForm을 HTML에 전달할 Model
     */
    @GetMapping("/user/password")
    public String changePage(Model model) {
        // redirect 후 FlashAttribute로 전달된 객체가 있다면 덮어쓰지 않는다.
        if (!model.containsAttribute("passwordChangeForm")) {
            // 빈 DTO를 넣어야 HTML의 th:object="${passwordChangeForm}"이 정상 동작한다.
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        }
        return "html/password-change";
    }

    /**
     * POST /user/password 요청을 처리한다.
     *
     * 처리 순서
     * 1. @Valid로 DTO 입력값을 검증한다.
     * 2. 세션에서 현재 직원 ID를 가져온다.
     * 3. UserService에서 현재 PW를 확인하고 새 PW를 BCrypt 해시로 변경한다.
     * 4. 성공하면 GET 요청으로 redirect한다.
     *
     * BindingResult는 반드시 @Valid 대상 바로 다음에 선언해야 해당 DTO의 오류를 받을 수 있다.
     */
    @PostMapping("/user/password")
    public String changePassword(
            Authentication authentication, // Spring Security Session에 저장된 로그인 정보
            @Valid @ModelAttribute("passwordChangeForm") PasswordChangeForm form, // HTML 폼 입력값을 담은 DTO
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Bean Validation에서 하나라도 실패했다면 Service와 DB를 호출하지 않는다.
        if (bindingResult.hasErrors()) {
            // 검증 실패 화면의 HTML에 사용자가 입력한 PW가 다시 포함되지 않도록 비운다.
            clearPasswordFields(form);
            return "html/password-change";
        }

        // Authentication#getName()에는 로그인에 사용한 직원 ID가 들어 있다.
        // Controller가 직접 BCrypt 처리나 SQL 호출을 하지 않고 Service에 요청한다.
        // 병원 선택용 임시 세션 값 대신 DB 인증을 완료한 직원의 소속 병원을 사용한다.
        if (!(authentication.getPrincipal() instanceof HospitalUserDetails userDetails)) {
            model.addAttribute("userError", "병원 로그인 정보가 없습니다. 다시 로그인해 주세요.");
            clearPasswordFields(form);
            return "html/password-change";
        }

        PasswordChangeResult result = userService.changePassword(
                userDetails.getHospitalId(), authentication.getName(), form.getCurrentPassword(), form.getNewPassword());

        if (result == PasswordChangeResult.CURRENT_PASSWORD_MISMATCH) {
            // rejectValue는 특정 DTO 필드에 서버 측 오류 메시지를 추가한다.
            // password-change.html의 th:errors="*{currentPassword}"에서 출력된다.
            bindingResult.rejectValue("currentPassword", "mismatch", "현재 비밀번호가 일치하지 않습니다.");
            clearPasswordFields(form);
            return "html/password-change";
        }
        if (result == PasswordChangeResult.USER_NOT_FOUND) {
            // 특정 필드 오류가 아니라 계정 전체 오류이므로 Model에 메시지를 넣는다.
            model.addAttribute("userError", "사용자 정보를 확인할 수 없습니다.");
            clearPasswordFields(form);
            return "html/password-change";
        }

        // 변경 성공 후 인증 정보와 기존 세션을 제거하고 새 비밀번호로 다시 로그인한다.
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        // 성공 여부만 쿼리 파라미터로 전달해 로그인 화면에서 변경 완료 안내를 표시한다.
        return "redirect:/login?passwordChanged";
    }

    /** 비밀번호 원문이 응답 HTML에 남지 않도록 DTO의 세 입력값을 제거한다. */
    private void clearPasswordFields(PasswordChangeForm form) {
        // null로 바꾸면 Thymeleaf가 렌더링할 때 password input의 value에 원문을 넣지 않는다.
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setPasswordConfirm(null);
    }
}
