// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.common.SessionConstants;

import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.domain.Role;
import com.aio.hospitalsafety.domain.ApprovalStatus;
import com.aio.hospitalsafety.config.HospitalUserDetails;
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

    /** 로그인 1단계 화면이 정상 렌더링되고 병원 ID 전송 주소를 포함하는지 확인한다. */
    @Test
    void redirectsToDomainPageWhenHospitalIsNotSelected() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-type"));
    }
    /** 비밀번호 변경 후 로그인 화면에 완료 안내가 표시되는지 확인한다. */
    @Test
    void showsLoginPageAfterHospitalSelection() throws Exception {
        when(hospitalService.findRegisteredHospital("HOSP01"))
                .thenReturn(Optional.of(new Hospital("HOSP01", "테스트병원")));

        mockMvc.perform(get("/login")
                        .sessionAttr(SessionConstants.HOSPITAL_DOMAIN, "HOSP01")
                        .sessionAttr(SessionConstants.LOGIN_ACCESS_TYPE, "user"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/auth/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/auth/login.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("userLoginKey")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/signup")));
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
                        .sessionAttr(LOGIN_HOSPITAL_NAME, "테스트병원")
                        .sessionAttr(SessionConstants.LOGIN_ACCESS_TYPE, "user"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/auth/login"))
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
                .andExpect(view().name("html/auth/password-change"))
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

    /** 본인 확인 방식이 미정인 비밀번호 재설정 안내 페이지는 로그인 없이 열려야 한다. */
    @Test
    void showsPasswordResetGuide() throws Exception {
        mockMvc.perform(get("/password/reset"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/auth/password-reset"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/auth/password-reset.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"reset-notice\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("소속 병동 관리자")));
    }
}


