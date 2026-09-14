// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.config.HospitalUserDetails;
import com.aio.hospitalsafety.dto.PasswordChangeForm;
import com.aio.hospitalsafety.service.UserService;
import com.aio.hospitalsafety.service.UserService.PasswordChangeResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class UserPasswordController {
    private final UserService userService;

    public UserPasswordController(UserService userService) {
        this.userService = userService;
    }

    /** 임시 비밀번호가 현재 로그인 계정의 비밀번호와 일치하는지 확인한다. */
    @PostMapping("/user/password/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> verifyInitialPassword(
            Authentication authentication,
            @RequestParam String initialPassword) {
        if (!(authentication != null
                && authentication.getPrincipal() instanceof HospitalUserDetails loginUser)) {
            return ResponseEntity.status(401)
                    .header("Cache-Control", "no-store")
                    .build();
        }

        boolean matches = userService.verifyInitialPassword(
                loginUser.getHospitalId(),
                loginUser.getUsername(),
                initialPassword
        );

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Map.of("matches", matches));
    }

    /** 비밀번호 변경 화면을 보여준다. */
    @GetMapping("/user/password")
    public String changePage(Authentication authentication, Model model) {
        if (!model.containsAttribute("passwordChangeForm")) {
            model.addAttribute(
                    "passwordChangeForm",
                    new PasswordChangeForm()
            );
        }

        model.addAttribute(
                "initialPasswordChange",
                isInitialPasswordChange(authentication)
        );

        return "html/settings/password-change";
    }

    /** 현재 비밀번호를 확인하고 새 비밀번호로 변경한다. */
    @PostMapping("/user/password")
    public String changePassword(
            Authentication authentication,
            @Valid @ModelAttribute("passwordChangeForm")
            PasswordChangeForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        boolean initialPasswordChange =
                isInitialPasswordChange(authentication);

        model.addAttribute(
                "initialPasswordChange",
                initialPasswordChange
        );

        if (bindingResult.hasErrors()) {
            clearPasswordFields(form);
            return "html/settings/password-change";
        }

        if (!(authentication != null
                && authentication.getPrincipal()
                instanceof HospitalUserDetails userDetails)) {
            model.addAttribute(
                    "userError",
                    "병원 로그인 정보가 없습니다. 다시 로그인해 주세요."
            );
            clearPasswordFields(form);
            return "html/settings/password-change";
        }

        PasswordChangeResult result = userService.changePassword(
                userDetails.getHospitalId(),
                userDetails.getUsername(),
                form.getCurrentPassword(),
                form.getNewPassword()
        );

        if (result == PasswordChangeResult.SAME_PASSWORD) {
            bindingResult.rejectValue(
                    "newPassword",
                    "same",
                    "현재 비밀번호와 다른 새 비밀번호를 입력해 주세요."
            );
            clearPasswordFields(form);
            return "html/settings/password-change";
        }

        if (result == PasswordChangeResult.CURRENT_PASSWORD_MISMATCH) {
            bindingResult.rejectValue(
                    "currentPassword",
                    "mismatch",
                    "현재 비밀번호가 일치하지 않습니다."
            );
            clearPasswordFields(form);
            return "html/settings/password-change";
        }

        if (result == PasswordChangeResult.USER_NOT_FOUND) {
            model.addAttribute(
                    "userError",
                    "사용자 정보를 확인할 수 없습니다."
            );
            clearPasswordFields(form);
            return "html/settings/password-change";
        }

        clearPasswordFields(form);

        if (initialPasswordChange) {
            request.changeSessionId();
            return "redirect:/dashboard";
        }

        String hospitalId = userDetails.getHospitalId();

        new SecurityContextLogoutHandler().logout(
                request,
                response,
                authentication
        );

        request.getSession(true).setAttribute(
                SessionConstants.HOSPITAL_DOMAIN,
                hospitalId
        );

        return "redirect:/login?role=USER&passwordChanged";
    }

    /** 현재 로그인 계정이 최초 비밀번호 변경 대상인지 확인한다. */
    private boolean isInitialPasswordChange(
            Authentication authentication) {
        if (!(authentication != null
                && authentication.getPrincipal()
                instanceof HospitalUserDetails userDetails)) {
            return false;
        }

        boolean employee = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_USER".equals(authority.getAuthority()));

        return employee && userService.isInitialUserPassword(
                userDetails.getHospitalId(),
                userDetails.getUsername()
        );
    }

    /** 비밀번호 원문이 다시 화면에 표시되지 않도록 제거한다. */
    private void clearPasswordFields(PasswordChangeForm form) {
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setPasswordConfirm(null);
    }
}