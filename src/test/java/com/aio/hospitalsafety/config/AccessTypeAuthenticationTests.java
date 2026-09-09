package com.aio.hospitalsafety.config;

import com.aio.hospitalsafety.common.SessionConstants;
import com.aio.hospitalsafety.domain.ApprovalStatus;
import com.aio.hospitalsafety.domain.Role;
import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccessTypeAuthenticationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void allowsAdminAccountForAdminAccessType() throws Exception {
        mockUser("admin", Role.ADMIN);

        mockMvc.perform(post("/login/user")
                        .with(csrf())
                        .sessionAttr(SessionConstants.LOGIN_ACCESS_TYPE, "admin")
                        .param("userLoginKey", "HOSP01|admin")
                        .param("password", "pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void allowsUserAccountForUserAccessType() throws Exception {
        mockUser("user01", Role.USER);

        mockMvc.perform(post("/login/user")
                        .with(csrf())
                        .sessionAttr(SessionConstants.LOGIN_ACCESS_TYPE, "user")
                        .param("userLoginKey", "HOSP01|user01")
                        .param("password", "pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void rejectsAdminAccountForUserAccessType() throws Exception {
        mockUser("admin", Role.ADMIN);

        mockMvc.perform(post("/login/user")
                        .with(csrf())
                        .sessionAttr(SessionConstants.LOGIN_ACCESS_TYPE, "user")
                        .param("userLoginKey", "HOSP01|admin")
                        .param("password", "pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/user?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void rejectsUserAccountForAdminAccessType() throws Exception {
        mockUser("user01", Role.USER);

        mockMvc.perform(post("/login/user")
                        .with(csrf())
                        .sessionAttr(SessionConstants.LOGIN_ACCESS_TYPE, "admin")
                        .param("userLoginKey", "HOSP01|user01")
                        .param("password", "pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/user?error"))
                .andExpect(unauthenticated());
    }

    private void mockUser(String userId, Role role) {
        User user = new User(userId, passwordEncoder.encode("pw"), "HOSP01", null,
                "테스트 사용자", role, ApprovalStatus.APPROVED, null, null);
        when(userMapper.findByHospitalIdAndUserId("HOSP01", userId)).thenReturn(Optional.of(user));
    }
}
