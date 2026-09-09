// PGH
package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.service.FindIdService;
import com.aio.hospitalsafety.service.FindIdService.FindIdRequestResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.aio.hospitalsafety.controller.AuthController.LOGIN_HOSPITAL_ID;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 아이디 찾기(이메일 인증) 화면의 3단계 MVC 흐름을 확인하는 통합 테스트다.
 *
 * FindIdService를 가짜 객체로 바꾸므로 실제 메일 발송이나 DB 조회는 일어나지 않는다.
 * 이 테스트의 목적은 URL, Session 상태 전이(요청 -> 인증 -> 결과), 화면 연결이
 * 올바른지 확인하는 것이다. 후보 조회/코드 발송 로직 자체는 FindIdServiceTests가 담당한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FindIdControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindIdService findIdService;

    @Test
    void redirectsToLoginWhenNoHospitalSelected() throws Exception {
        mockMvc.perform(get("/id/find"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void showsFindIdRequestForm() throws Exception {
        mockMvc.perform(get("/id/find").sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/find-id"))
                .andExpect(content().string(containsString("id=\"employeeName\"")))
                .andExpect(content().string(containsString("id=\"email\"")));
    }

    @Test
    void showsGenericErrorWhenNoAccountMatches() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "없는사람", "nobody@example.com"))
                .thenReturn(FindIdRequestResult.notFound());

        mockMvc.perform(post("/id/find/send")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .param("employeeName", "없는사람")
                        .param("email", "nobody@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/find-id"))
                .andExpect(content().string(containsString("class=\"label-error\"")))
                .andExpect(content().string(containsString("* 잘못된 내용입니다!")));
    }

    @Test
    void showsErrorWhenMailSendingFails() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.sendFailed());

        mockMvc.perform(post("/id/find/send")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("인증 메일을 보내지 못했습니다.")));
    }

    @Test
    void verifyWithoutPriorRequestSendsBackToRequestStep() throws Exception {
        mockMvc.perform(post("/id/find/verify")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("인증 시간이 만료되었습니다.")))
                .andExpect(content().string(containsString("id=\"employeeName\"")));
    }

    @Test
    void verifiesCorrectCodeAndShowsFoundUserId() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.success("EMP001", "123456"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");

        mockMvc.perform(post("/id/find/send")
                        .with(csrf())
                        .session(session)
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"code\"")));

        mockMvc.perform(post("/id/find/verify")
                        .with(csrf())
                        .session(session)
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EMP001")));
    }

    @Test
    void showsErrorWhenCodeIsIncorrect() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.success("EMP001", "123456"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");

        mockMvc.perform(post("/id/find/send")
                        .with(csrf())
                        .session(session)
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"));

        mockMvc.perform(post("/id/find/verify")
                        .with(csrf())
                        .session(session)
                        .param("code", "000000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("* 잘못된 내용입니다!")));
    }

    @Test
    void locksOutAfterTooManyWrongAttempts() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.success("EMP001", "123456"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");

        mockMvc.perform(post("/id/find/send")
                        .with(csrf())
                        .session(session)
                        .param("employeeName", "홍길동")
                        .param("email", "hong@example.com"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/id/find/verify")
                    .with(csrf())
                    .session(session)
                    .param("code", "000000"));
        }

        mockMvc.perform(post("/id/find/verify")
                        .with(csrf())
                        .session(session)
                        .param("code", "000000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("인증 시도 횟수를 초과했습니다.")));
    }
}
