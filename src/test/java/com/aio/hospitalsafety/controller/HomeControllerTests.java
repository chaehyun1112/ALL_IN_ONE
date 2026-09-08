package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.dto.HospitalDto;
import com.aio.hospitalsafety.mapper.HospitalMapper;
import com.aio.hospitalsafety.service.HospitalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HomeControllerTests {

    private HospitalMapper hospitalMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        hospitalMapper = mock(HospitalMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new HomeController(new HospitalService(hospitalMapper)), new AccessTypeController()).build();
    }

    @Test
    void registeredDomainOpensAccessTypeAndKeepsHospitalForLogin() throws Exception {
        when(hospitalMapper.findHospitalByDomain("test"))
                .thenReturn(new HospitalDto("test", "늘푸른병원"));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/domain").param("hospitalDomain", " test ").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-type"))
                .andExpect(request().sessionAttribute(SessionConstants.HOSPITAL_DOMAIN, "test"));
        mockMvc.perform(get("/access-type").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("html/access-type"));
        mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("html/login"))
                .andExpect(model().attribute("hospitalName", "늘푸른병원"));
    }

    @Test
    void unknownDomainShowsErrorAndClearsPreviousSelection() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConstants.HOSPITAL_DOMAIN, "test");
        mockMvc.perform(post("/domain").param("hospitalDomain", "unknown").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("html/index"))
                .andExpect(model().attributeExists("domainError"))
                .andExpect(model().attribute("hospitalDomain", "unknown"))
                .andExpect(request().sessionAttributeDoesNotExist(SessionConstants.HOSPITAL_DOMAIN));
    }

    @Test
    void blankDomainDoesNotQueryDatabase() throws Exception {
        mockMvc.perform(post("/domain").param("hospitalDomain", "   "))
                .andExpect(view().name("html/index"))
                .andExpect(model().attributeExists("domainError"));
        verifyNoInteractions(hospitalMapper);
    }

    @Test
    void databaseFailureShowsRetryMessage() throws Exception {
        when(hospitalMapper.findHospitalByDomain("test"))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));
        mockMvc.perform(post("/domain").param("hospitalDomain", "test"))
                .andExpect(view().name("html/index"))
                .andExpect(model().attribute("domainError",
                        "병원 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    void loginWithoutHospitalReturnsToDomainScreen() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(redirectedUrl("/"));
        verifyNoInteractions(hospitalMapper);
    }
}
