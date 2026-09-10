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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 아이디 찾기(이메일 인증) 화면의 MVC 흐름을 확인하는 통합 테스트다.
 *
 * find-id.html 한 화면 안에서 발송/확인이 새로고침 없는 AJAX(JSON) 요청으로 처리되므로,
 * 이 테스트도 화면 전환이 아니라 /id/find/send-code, /id/find/verify-code가 돌려주는
 * JSON 응답을 확인한다. FindIdService를 가짜 객체로 바꾸므로 실제 메일 발송이나 DB 조회는
 * 일어나지 않는다. 후보 조회/코드 발송 로직 자체는 FindIdServiceTests가 담당한다.
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
    void showsFindIdPage() throws Exception {
        mockMvc.perform(get("/id/find").sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01"))
                .andExpect(status().isOk())
                .andExpect(view().name("html/find-id"))
                .andExpect(content().string(containsString("id=\"employeeName\"")))
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("id=\"sendCodeBtn\"")));
    }

    @Test
    void sendCodeWithoutHospitalSessionIsUnauthorized() throws Exception {
        mockMvc.perform(post("/id/find/send-code")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"employeeName\":\"홍길동\",\"email\":\"hong@example.com\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    void sendsCodeWhenIdentityMatches() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.success("EMP001", "123456"));

        mockMvc.perform(post("/id/find/send-code")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .contentType("application/json")
                        .content("{\"employeeName\":\"홍길동\",\"email\":\"hong@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void showsGenericStatusWhenNoAccountMatches() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "없는사람", "nobody@example.com"))
                .thenReturn(FindIdRequestResult.notFound());

        mockMvc.perform(post("/id/find/send-code")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .contentType("application/json")
                        .content("{\"employeeName\":\"없는사람\",\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void showsErrorWhenMailSendingFails() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.sendFailed());

        mockMvc.perform(post("/id/find/send-code")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .contentType("application/json")
                        .content("{\"employeeName\":\"홍길동\",\"email\":\"hong@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEND_FAILED"));
    }

    @Test
    void verifyWithoutPriorSendIsExpired() throws Exception {
        mockMvc.perform(post("/id/find/verify-code")
                        .with(csrf())
                        .sessionAttr(LOGIN_HOSPITAL_ID, "HOSP01")
                        .contentType("application/json")
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void verifiesCorrectCodeAndReturnsFoundUserId() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.success("EMP001", "123456"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");

        mockMvc.perform(post("/id/find/send-code")
                .with(csrf())
                .session(session)
                .contentType("application/json")
                .content("{\"employeeName\":\"홍길동\",\"email\":\"hong@example.com\"}"));

        mockMvc.perform(post("/id/find/verify-code")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.userId").value("EMP001"));
    }

    @Test
    void showsErrorWhenCodeIsIncorrect() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.success("EMP001", "123456"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");

        mockMvc.perform(post("/id/find/send-code")
                .with(csrf())
                .session(session)
                .contentType("application/json")
                .content("{\"employeeName\":\"홍길동\",\"email\":\"hong@example.com\"}"));

        mockMvc.perform(post("/id/find/verify-code")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID"));
    }

    @Test
    void locksOutAfterTooManyWrongAttempts() throws Exception {
        when(findIdService.requestVerificationCode("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(FindIdRequestResult.success("EMP001", "123456"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LOGIN_HOSPITAL_ID, "HOSP01");

        mockMvc.perform(post("/id/find/send-code")
                .with(csrf())
                .session(session)
                .contentType("application/json")
                .content("{\"employeeName\":\"홍길동\",\"email\":\"hong@example.com\"}"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/id/find/verify-code")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content("{\"code\":\"000000\"}"));
        }

        mockMvc.perform(post("/id/find/verify-code")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }
}
