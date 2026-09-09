package com.aio.hospitalsafety.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aio.hospitalsafety.dto.Signup;
import com.aio.hospitalsafety.dto.WardOption;
import com.aio.hospitalsafety.mapper.SignupMapper;

@Service
public class SignupService {

    private final SignupMapper signupMapper;
    private final PasswordEncoder passwordEncoder;

    public SignupService(
            SignupMapper signupMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.signupMapper = signupMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // 사용자 아이디 사용 가능 여부 확인
    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {

        if (userId == null || userId.isBlank()) {
            return false;
        }

        String trimmedUserId = userId.trim();

        return !signupMapper.existsUserId(trimmedUserId);
    }

    // 현재 병원에 속한 병동 목록 조회
    @Transactional(readOnly = true)
    public List<WardOption> getWardsByHospital(String hospDivId) {

        if (!signupMapper.existsHospital(hospDivId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 병원입니다."
            );
        }

        return signupMapper.findWardsByHospital(hospDivId);
    }

    // 사용자 회원가입 처리
    @Transactional
    public void signupUser(Signup signup, String hospDivId) {

        String userId = signup.userId().trim();
        String userName = signup.userName().trim();

        // 현재 접속한 병원이 실제로 존재하는지 확인
        if (!signupMapper.existsHospital(hospDivId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 병원입니다."
            );
        }

        // 사용자 아이디 중복 확인
        if (signupMapper.existsUserId(userId)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다."
            );
        }

        // 비밀번호와 비밀번호 확인이 같은지 검사
        if (!signup.password().equals(signup.passwordConfirm())) {
            throw new IllegalArgumentException(
                    "비밀번호가 일치하지 않습니다."
            );
        }

        // 선택한 병동이 현재 병원 소속인지 확인
        if (signup.wardId() != null
                && !signupMapper.existsWardInHospital(
                        hospDivId,
                        signup.wardId()
                )) {
            throw new IllegalArgumentException(
                    "해당 병원에 속하지 않는 병동입니다."
            );
        }

        // BCrypt에서 처리할 수 있는 비밀번호 길이 확인
        int passwordBytes =
                signup.password()
                        .getBytes(StandardCharsets.UTF_8)
                        .length;

        if (passwordBytes > 72) {
            throw new IllegalArgumentException(
                    "비밀번호가 너무 깁니다."
            );
        }

        // 비밀번호 암호화
        String encodedPassword =
                passwordEncoder.encode(signup.password());

        // 사용자 정보 저장
        int insertedRows = signupMapper.insertUser(
                userId,
                encodedPassword,
                userName,
                hospDivId,
                signup.wardId()
        );

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "회원가입 처리 중 오류가 발생했습니다."
            );
        }
    }
}
