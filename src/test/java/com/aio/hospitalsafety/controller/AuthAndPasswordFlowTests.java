// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.domain.Role;
import com.aio.hospitalsafety.domain.ApprovalStatus;
import com.aio.hospitalsafety.config.HospitalUserDetails;
import com.aio.hospitalsafety.service.PasswordResetService;
import com.aio.hospitalsafety.service.PasswordResetService.IdentifyResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import com.aio.hospitalsafety.service.HospitalService;
import com.aio.hospitalsafety.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static com.aio.hospitalsafety.controller.AuthController.LOGIN_HOSPITAL_ID;
import static com.aio.hospitalsafety.controller.AuthController.LOGIN_HOSPITAL_NAME;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 로그인과 비밀번호 화면의 MVC 흐름을 한꺼번에 확인하는 통합 테스트다.
 *
 * 실제 PostgreSQL 대신 Service를 가짜 객체(Mock)로 바꾸므로 AIO DB를 생성하거나 수정하지 않는다.
 * 이 테스트의 목적은 URL, Controller, Session, Thymeleaf 화면, Spring Security가 서로
 * 올바르게 연결되어 있는지 확인하는 것이다. 비밀번호 해시와 DB UPDATE는 UserServiceTests가 담당한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndPasswordFlowTests {

    // MockMvc는 실제 브라우저를 띄우지 않고도 Spring MVC에 HTTP 요청을 보내는 테스트 도구다.
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean은 실제 HospitalService Bean 대신 테스트용 가짜 Bean을 Spring에 등록한다.
    @MockitoBean
    private HospitalService hospitalService;

    // 비밀번호 변경 Controller가 호출할 결과를 테스트마다 원하는 값으로 지정하기 위한 가짜 Service다.
    @MockitoBean
    private UserService userService;

    // 비밀번호 재설정(이메일 인증) Controller가 호출할 결과를 테스트마다 지정하기 위한 가짜 Service다.
    @MockitoBean
    private PasswordResetService passwordResetService;

    /** 로그인 1단계 화면이 정상 렌더링되고 병원 ID 전송 주소를 포함하는지 확인한다. */
    @Test
    void showsHospitalLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/login/hospital")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/auth/login.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"login-card hospital-card\"")));
    }

    /**
     * 회귀 테스트: GET /login은 이미 선택된 병원 세션 값을 지우면 안 된다.
     *
     * user-login.html의 "다른 병원 선택" 링크(href="/login")가 브라우저 프리페치로
     * 사용자가 클릭하지 않아도 조용히 호출될 수 있는데, 예전 구현은 이 GET에서
     * LOGIN_HOSPITAL_ID/NAME을 무조건 지워서 비밀번호 재설정 등 이후 화면이
     * 세션 없음으로 오인해 로그인 화면으로 튕기는 버그가 있었다.
     */
    @Test
    void gettingLoginPageDoesNotClearExistingHospitalSelection() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");
        session.setAttribute(LOGIN_HOSPITAL_NAME, "테스트병원");

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(session.getAttribute(LOGIN_HOSPITAL_ID)).isEqualTo("HOSP01");
        org.assertj.core.api.Assertions.assertThat(session.getAttribute(LOGIN_HOSPITAL_NAME)).isEqualTo("테스트병원");
    }

    /** 비밀번호 변경 후 로그인 화면에 완료 안내가 표시되는지 확인한다. */
    @Test
    void showsPasswordChangedMessageOnLoginPage() throws Exception {
        mockMvc.perform(get("/login").param("passwordChanged", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.")));
    }

    /** 등록 병원을 선택하면 병원 정보를 Session에 저장하고 사용자 로그인 화면으로 이동한다. */
    @Test
    void movesToUserLoginAfterHospitalSelection() throws Exception {
        when(hospitalService.findRegisteredHospital("HOSP01"))
                .thenReturn(Optional.of(new Hospital("HOSP01", "테스트병원")));

        // Spring Security의 CSRF 보호가 켜져 있으므로 POST 테스트에도 csrf() 토큰을 함께 보낸다.
        mockMvc.perform(post("/login/hospital")
                        .with(csrf())
                        .param("hospitalId", "HOSP01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/user"));

        // 두 번째 화면은 첫 단계에서 저장한 병원 Session 값이 있을 때만 열려야 한다.
        mockMvc.perform(get("/login/user")
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .sessionAttr(LOGIN_HOSPITAL_NAME, "테스트병원"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/user-login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("userLoginKey")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("테스트병원")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("비밀번호 재설정")));
    }

    /** 비로그인 사용자가 사용자 전용 비밀번호 변경 주소에 접근하면 로그인 화면으로 보낸다. */
    @Test
    void protectsUserPasswordPage() throws Exception {
        mockMvc.perform(get("/user/password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /** 로그인 사용자의 비밀번호 변경 화면에 새 디자인과 검증 스크립트가 적용되는지 확인한다. */
    @Test
    void showsStyledPasswordChangePage() throws Exception {
        mockMvc.perform(get("/user/password")
                        .with(user(new HospitalUserDetails(new User("USER01", "unused", "HOSP01", null,
                                "직원", Role.USER, ApprovalStatus.APPROVED, null, null)))))
                .andExpect(status().isOk())
                .andExpect(view().name("html/password-change"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/auth/password-reset.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"reset-form\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"reset-submit\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/JS/password-rest/Password_check.js")));
    }

    /** 인증된 사용자의 올바른 입력을 UserService에 전달하고 성공하면 PRG 방식으로 이동한다. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"OTHER_HOSPITAL"})
    void changesAuthenticatedUserPassword(String selectedHospital) throws Exception {
        MockHttpSession session = new MockHttpSession();
        if (selectedHospital != null) session.setAttribute(LOGIN_HOSPITAL_ID, selectedHospital);
        when(userService.changePassword(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(UserService.PasswordChangeResult.SUCCESS);

        mockMvc.perform(post("/user/password")
                        // 병원 선택값이 없거나 달라도 인증된 직원의 소속 병원을 사용해야 한다.
                        .with(user(new HospitalUserDetails(new User("USER01", "unused", "HOSP01", null, "직원", Role.USER, ApprovalStatus.APPROVED, null, null))))
                        .with(csrf())
                        .session(session)
                        .param("currentPassword", "Current123")
                        .param("newPassword", "NewPassword123")
                        .param("passwordConfirm", "NewPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged"))
                .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated());
        org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
        verify(userService).changePassword("HOSP01", "USER01", "Current123", "NewPassword123");
    }

    /** 병원을 먼저 선택하지 않고 재설정 화면에 접근하면 로그인(병원 선택) 화면으로 보낸다. */
    @Test
    void redirectsPasswordResetToLoginWhenNoHospitalSelected() throws Exception {
        mockMvc.perform(get("/password/reset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /** 병원 선택(Session)이 있는 상태에서는 아이디+이름+이메일 확인 폼이 로그인 없이 열려야 한다. */
    @Test
    void showsPasswordResetForm() throws Exception {
        mockMvc.perform(get("/password/reset").sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/password-reset"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/auth/password-reset.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"reset-form\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"employeeId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"employeeName\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"email\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("소속 병동 관리자")));
    }

    /** 아이디+이름+이메일이 일치하면 인증코드를 발송하고 2단계 화면으로 이동한다. */
    @Test
    void sendsVerificationCodeWhenIdentityMatches() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");
        when(passwordResetService.verifyIdentityAndSendCode("HOSP01", "USER01", "홍길동", "hong@example.com"))
                .thenReturn(IdentifyResult.success("USER01", "123456"));

        mockMvc.perform(post("/password/reset/verify-identity")
                        .with(csrf())
                        .session(session)
                        .param("employeeId", "USER01")
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/password-reset"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"code\"")));
        verify(passwordResetService).verifyIdentityAndSendCode("HOSP01", "USER01", "홍길동", "hong@example.com");
    }

    /** 아이디/이름/이메일 중 하나라도 일치하지 않으면 어느 쪽이 틀렸는지 알려주지 않고 같은 오류만 보여준다. */
    @Test
    void showsGenericErrorWhenIdentityDoesNotMatch() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");
        when(passwordResetService.verifyIdentityAndSendCode(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(IdentifyResult.notFound());

        mockMvc.perform(post("/password/reset/verify-identity")
                        .with(csrf())
                        .session(session)
                        .param("employeeId", "USER01")
                        .param("employeeName", "다른이름")
                        .param("email", "hong@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/password-reset"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"label-error\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("* 잘못된 내용입니다!")));
    }

    /** 인증코드가 일치하면 3단계(새 비밀번호 설정) 화면으로 이동한다. */
    @Test
    void movesToNewPasswordStepWhenCodeMatches() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");
        when(passwordResetService.verifyIdentityAndSendCode("HOSP01", "USER01", "홍길동", "hong@example.com"))
                .thenReturn(IdentifyResult.success("USER01", "123456"));

        mockMvc.perform(post("/password/reset/verify-identity")
                        .with(csrf())
                        .session(session)
                        .param("employeeId", "USER01")
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"));

        mockMvc.perform(post("/password/reset/verify-code")
                        .with(csrf())
                        .session(session)
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/password-reset"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"newPassword\"")));
    }

    /** 인증코드가 틀리면 인증코드 라벨 옆에 오류를 보여준다. */
    @Test
    void showsGenericErrorWhenCodeDoesNotMatch() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");
        when(passwordResetService.verifyIdentityAndSendCode("HOSP01", "USER01", "홍길동", "hong@example.com"))
                .thenReturn(IdentifyResult.success("USER01", "123456"));

        mockMvc.perform(post("/password/reset/verify-identity")
                        .with(csrf())
                        .session(session)
                        .param("employeeId", "USER01")
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"));

        mockMvc.perform(post("/password/reset/verify-code")
                        .with(csrf())
                        .session(session)
                        .param("code", "000000"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("* 잘못된 내용입니다!")));
    }

    /** 이메일 인증 없이 곧바로 비밀번호 저장 URL에 접근하면 1단계로 되돌아간다. */
    @Test
    void rejectsPasswordSaveWithoutEmailVerification() throws Exception {
        mockMvc.perform(post("/password/reset")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .param("newPassword", "NewPassword123")
                        .param("passwordConfirm", "NewPassword123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("이메일 인증을 먼저 완료해 주세요.")));
    }

    /** 이메일 인증까지 끝난 뒤에만 실제로 비밀번호를 저장하고 로그인 화면으로 보낸다. */
    @Test
    void resetsPasswordAfterEmailVerification() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");
        when(passwordResetService.verifyIdentityAndSendCode("HOSP01", "USER01", "홍길동", "hong@example.com"))
                .thenReturn(IdentifyResult.success("USER01", "123456"));

        mockMvc.perform(post("/password/reset/verify-identity")
                        .with(csrf())
                        .session(session)
                        .param("employeeId", "USER01")
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"));
        mockMvc.perform(post("/password/reset/verify-code")
                        .with(csrf())
                        .session(session)
                        .param("code", "123456"));

        mockMvc.perform(post("/password/reset")
                        .with(csrf())
                        .session(session)
                        .param("newPassword", "NewPassword123")
                        .param("passwordConfirm", "NewPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged"));
        verify(passwordResetService).resetPassword("USER01", "NewPassword123");
    }
}
