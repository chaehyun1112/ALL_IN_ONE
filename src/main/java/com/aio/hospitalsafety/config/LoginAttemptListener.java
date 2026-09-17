package com.aio.hospitalsafety.config;

import com.aio.hospitalsafety.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 로그인 브루트포스 방어를 위해 인증 성공/실패 이벤트를 감시한다.
 *
 * usernameParameter는 "hospitalId|userId" 형태(SecurityConfig의 userDetailsService와 동일한
 * 형식)이므로, 실패 시에는 이 값을 직접 분해해서 병원/직원 아이디를 얻는다. 성공 시에는
 * 이미 인증이 끝나 principal이 HospitalUserDetails로 바뀌어 있으므로 그대로 사용한다.
 */
@Component
public class LoginAttemptListener {

    private final UserService userService;

    public LoginAttemptListener(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (!(principal instanceof String userLoginKey)) {
            return;
        }

        String[] parts = userLoginKey.split("\\|", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return;
        }

        userService.registerFailedLogin(parts[0].trim(), parts[1].trim());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (!(principal instanceof HospitalUserDetails loginUser)) {
            return;
        }

        userService.resetFailedLogin(
                loginUser.getHospitalId(),
                loginUser.getUsername()
        );
    }
}
