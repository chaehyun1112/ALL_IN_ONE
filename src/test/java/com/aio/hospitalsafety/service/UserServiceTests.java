// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.domain.ApprovalStatus;
import com.aio.hospitalsafety.domain.Role;
import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserMapper userMapper;

    @Test
    void storesOnlyBcryptHashWhenPasswordChanges() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserService service = new UserService(userMapper, encoder);
        User user = new User(
                "EMP001", encoder.encode("Current123"), "HOSP01", 1L, "홍길동",
                Role.USER, ApprovalStatus.APPROVED, Instant.now(), null);
        when(userMapper.findByHospitalIdAndUserId("HOSP01", "EMP001")).thenReturn(Optional.of(user));
        when(userMapper.updatePassword(eq("EMP001"), anyString())).thenReturn(1);

        assertThat(service.changePassword("HOSP01", "EMP001", "Current123", "NewPassword123"))
                .isEqualTo(UserService.PasswordChangeResult.SUCCESS);

        ArgumentCaptor<String> savedPassword = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updatePassword(eq("EMP001"), savedPassword.capture());
        assertThat(savedPassword.getValue()).isNotEqualTo("NewPassword123");
        assertThat(encoder.matches("NewPassword123", savedPassword.getValue())).isTrue();
    }

    @Test
    void doesNotUpdateWhenCurrentPasswordIsWrong() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserService service = new UserService(userMapper, encoder);
        User user = new User(
                "EMP001", encoder.encode("Current123"), "HOSP01", 1L, "홍길동",
                Role.USER, ApprovalStatus.APPROVED, Instant.now(), null);
        when(userMapper.findByHospitalIdAndUserId("HOSP01", "EMP001")).thenReturn(Optional.of(user));

        assertThat(service.changePassword("HOSP01", "EMP001", "WrongPassword", "NewPassword123"))
                .isEqualTo(UserService.PasswordChangeResult.CURRENT_PASSWORD_MISMATCH);
        verify(userMapper, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void resetsPasswordWhenEmployeeIdAndNameMatch() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserService service = new UserService(userMapper, encoder);
        User user = new User(
                "EMP001", encoder.encode("Old12345"), "HOSP01", 1L, "홍길동",
                Role.USER, ApprovalStatus.APPROVED, Instant.now(), null);
        when(userMapper.findByHospitalIdAndUserId("HOSP01", "EMP001")).thenReturn(Optional.of(user));
        when(userMapper.updatePassword(eq("EMP001"), anyString())).thenReturn(1);

        assertThat(service.resetPassword("HOSP01", "EMP001", "홍길동", "NewPassword123"))
                .isEqualTo(UserService.PasswordResetResult.SUCCESS);

        ArgumentCaptor<String> savedPassword = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updatePassword(eq("EMP001"), savedPassword.capture());
        assertThat(savedPassword.getValue()).isNotEqualTo("NewPassword123");
        assertThat(encoder.matches("NewPassword123", savedPassword.getValue())).isTrue();
    }

    @Test
    void doesNotResetPasswordWhenNameDoesNotMatch() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserService service = new UserService(userMapper, encoder);
        User user = new User(
                "EMP001", encoder.encode("Old12345"), "HOSP01", 1L, "홍길동",
                Role.USER, ApprovalStatus.APPROVED, Instant.now(), null);
        when(userMapper.findByHospitalIdAndUserId("HOSP01", "EMP001")).thenReturn(Optional.of(user));

        assertThat(service.resetPassword("HOSP01", "EMP001", "다른이름", "NewPassword123"))
                .isEqualTo(UserService.PasswordResetResult.IDENTITY_MISMATCH);
        verify(userMapper, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void doesNotResetPasswordWhenEmployeeIdDoesNotExist() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserService service = new UserService(userMapper, encoder);
        when(userMapper.findByHospitalIdAndUserId("HOSP01", "NOBODY")).thenReturn(Optional.empty());

        assertThat(service.resetPassword("HOSP01", "NOBODY", "홍길동", "NewPassword123"))
                .isEqualTo(UserService.PasswordResetResult.IDENTITY_MISMATCH);
        verify(userMapper, never()).updatePassword(anyString(), anyString());
    }
}
