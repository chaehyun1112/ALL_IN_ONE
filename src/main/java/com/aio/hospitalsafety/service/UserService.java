// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 최초 로그인 비밀번호 변경이 필요한 계정인지 확인한다. */
    @Transactional(readOnly = true)
    public boolean isInitialUserPassword(String hospitalId, String userId) {
        return Boolean.TRUE.equals(
                userMapper.isInitialUserPassword(hospitalId, userId)
        );
    }

    /**
     * 로그인 브루트포스 방어: 비밀번호를 틀릴 때마다 호출해 실패 횟수를 올리고,
     * 5회 단위로 걸릴 때마다 15분 잠금을 새로 건다.
     */
    @Transactional
    public void registerFailedLogin(String hospitalId, String userId) {
        userMapper.registerFailedLogin(hospitalId, userId);
    }

    /** 로그인에 성공하면 실패 횟수와 잠금을 초기화한다. */
    @Transactional
    public void resetFailedLogin(String hospitalId, String userId) {
        userMapper.resetFailedLogin(hospitalId, userId);
    }

    /** 로그인한 계정의 임시 비밀번호와 사용자가 입력한 값이 일치하는지 확인한다. */
    @Transactional(readOnly = true)
    public boolean verifyInitialPassword(
            String hospitalId,
            String userId,
            String initialPassword) {
        if (initialPassword == null
                || initialPassword.isBlank()
                || initialPassword.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                ).length > 72) {
            return false;
        }

        if (!isInitialUserPassword(hospitalId, userId)) {
            return false;
        }

        return userMapper.findByHospitalIdAndUserId(hospitalId, userId)
                .map(user -> passwordEncoder.matches(
                        initialPassword,
                        user.passwordHash()
                ))
                .orElse(false);
    }

    /** 현재 비밀번호를 확인하고 새 비밀번호로 변경한다. */
    @Transactional
    public PasswordChangeResult changePassword(
            String hospitalId,
            String userId,
            String currentPassword,
            String newPassword) {
        User user = userMapper.findByHospitalIdAndUserId(
                hospitalId,
                userId
        ).orElse(null);

        if (user == null) {
            return PasswordChangeResult.USER_NOT_FOUND;
        }

        if (!passwordEncoder.matches(
                currentPassword,
                user.passwordHash()
        )) {
            return PasswordChangeResult.CURRENT_PASSWORD_MISMATCH;
        }

        if (passwordEncoder.matches(
                newPassword,
                user.passwordHash()
        )) {
            return PasswordChangeResult.SAME_PASSWORD;
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);

        if (userMapper.updatePassword(
                user.userId(),
                newPasswordHash
        ) != 1) {
            throw new IllegalStateException("비밀번호 변경에 실패했습니다.");
        }

        return PasswordChangeResult.SUCCESS;
    }

    public enum PasswordChangeResult {
        SUCCESS,
        SAME_PASSWORD,
        CURRENT_PASSWORD_MISMATCH,
        USER_NOT_FOUND
    }
}