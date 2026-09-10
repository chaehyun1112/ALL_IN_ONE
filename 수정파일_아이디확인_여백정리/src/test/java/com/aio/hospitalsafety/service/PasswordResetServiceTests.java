// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.mapper.UserMapper;
import com.aio.hospitalsafety.service.PasswordResetService.IdentifyResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MailService mailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void sendsSixDigitCodeWhenEmployeeIdIsAmongMatches() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(List.of("EMP001"));
        PasswordResetService service = new PasswordResetService(userMapper, mailService, passwordEncoder);

        IdentifyResult result = service.verifyIdentityAndSendCode("HOSP01", "EMP001", "홍길동", "hong@example.com");

        assertThat(result.status()).isEqualTo(IdentifyResult.Status.SUCCESS);
        assertThat(result.userId()).isEqualTo("EMP001");
        assertThat(result.code()).matches("^[0-9]{6}$");

        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetVerificationCode(anyString(), sentCode.capture());
        assertThat(sentCode.getValue()).isEqualTo(result.code());
    }

    @Test
    void returnsNotFoundWhenEmployeeIdIsNotAmongMatches() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(List.of("EMP002"));
        PasswordResetService service = new PasswordResetService(userMapper, mailService, passwordEncoder);

        IdentifyResult result = service.verifyIdentityAndSendCode("HOSP01", "EMP001", "홍길동", "hong@example.com");

        assertThat(result.status()).isEqualTo(IdentifyResult.Status.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenNoAccountMatches() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "없는사람", "nobody@example.com"))
                .thenReturn(List.of());
        PasswordResetService service = new PasswordResetService(userMapper, mailService, passwordEncoder);

        IdentifyResult result = service.verifyIdentityAndSendCode("HOSP01", "EMP001", "없는사람", "nobody@example.com");

        assertThat(result.status()).isEqualTo(IdentifyResult.Status.NOT_FOUND);
    }

    @Test
    void returnsSendFailedWhenMailSendingThrows() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(List.of("EMP001"));
        doThrow(new MailAuthenticationException("smtp auth failed"))
                .when(mailService).sendPasswordResetVerificationCode(anyString(), anyString());
        PasswordResetService service = new PasswordResetService(userMapper, mailService, passwordEncoder);

        IdentifyResult result = service.verifyIdentityAndSendCode("HOSP01", "EMP001", "홍길동", "hong@example.com");

        assertThat(result.status()).isEqualTo(IdentifyResult.Status.SEND_FAILED);
    }

    @Test
    void employeeIdMatchesNameReturnsTrueWhenIdAndNameBothMatch() {
        when(userMapper.existsByHospitalIdAndUserIdAndUserName("HOSP01", "EMP001", "홍길동"))
                .thenReturn(true);
        PasswordResetService service = new PasswordResetService(userMapper, mailService, passwordEncoder);

        assertThat(service.employeeIdMatchesName("HOSP01", "EMP001", "홍길동")).isTrue();
    }

    @Test
    void employeeIdMatchesNameReturnsFalseWhenNameDoesNotMatch() {
        when(userMapper.existsByHospitalIdAndUserIdAndUserName("HOSP01", "EMP001", "다른이름"))
                .thenReturn(false);
        PasswordResetService service = new PasswordResetService(userMapper, mailService, passwordEncoder);

        assertThat(service.employeeIdMatchesName("HOSP01", "EMP001", "다른이름")).isFalse();
    }

    @Test
    void storesOnlyBcryptHashWhenPasswordIsReset() {
        when(userMapper.updatePassword(eq("EMP001"), anyString())).thenReturn(1);
        PasswordResetService service = new PasswordResetService(userMapper, mailService, passwordEncoder);

        service.resetPassword("EMP001", "NewPassword123");

        ArgumentCaptor<String> savedPassword = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updatePassword(eq("EMP001"), savedPassword.capture());
        assertThat(savedPassword.getValue()).isNotEqualTo("NewPassword123");
        assertThat(passwordEncoder.matches("NewPassword123", savedPassword.getValue())).isTrue();
    }
}
