// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.mapper.UserMapper;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * 비밀번호 재설정(이메일 인증) 기능을 담당한다.
 *
 * 처리 순서
 * 1. 직원 ID + 이름 + 이메일이 모두 일치하는지 확인한다(어느 값이 틀렸는지는 알려주지 않는다).
 * 2. 일치하면 6자리 인증코드를 만들어 해당 이메일로 발송한다.
 * 3. 코드 검증과 만료 처리는 UserPasswordController가 HttpSession에 저장해 담당한다
 *    (이 Service는 상태를 갖지 않는다).
 * 4. 코드 검증까지 끝난 뒤에만 Controller가 resetPassword()를 호출해 실제 비밀번호를 바꾼다.
 */
@Service
public class PasswordResetService {

    private final UserMapper userMapper;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserMapper userMapper, MailService mailService, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public IdentifyResult verifyIdentityAndSendCode(
            String hospitalId, String employeeId, String employeeName, String email) {
        List<String> candidates = userMapper.findMatchingUserIdsForFindId(hospitalId, employeeName, email);
        // 이름+이메일이 일치하는 계정 중에 입력한 직원 ID가 포함돼 있어야 3가지가 모두 일치한 것이다.
        if (!candidates.contains(employeeId)) {
            return IdentifyResult.notFound();
        }

        String code = generateSixDigitCode();
        try {
            mailService.sendPasswordResetVerificationCode(email, code);
        } catch (MailException exception) {
            return IdentifyResult.sendFailed();
        }

        return IdentifyResult.success(employeeId, code);
    }

    /**
     * '아이디 확인' 버튼용이다. 아이디만으로 존재 여부를 알려주면 계정 열거 공격에
     * 악용될 수 있어, 아이디+이름이 함께 일치할 때만 true를 반환한다.
     */
    @Transactional(readOnly = true)
    public boolean employeeIdMatchesName(String hospitalId, String employeeId, String employeeName) {
        return userMapper.existsByHospitalIdAndUserIdAndUserName(hospitalId, employeeId, employeeName);
    }

    /** 이메일 인증코드 검증까지 끝난 직원 ID에 대해서만 호출해야 한다. */
    @Transactional
    public void resetPassword(String employeeId, String newPassword) {
        String newPasswordHash = passwordEncoder.encode(newPassword);
        if (userMapper.updatePassword(employeeId, newPasswordHash) != 1) {
            throw new IllegalStateException("비밀번호 재설정에 실패했습니다.");
        }
    }

    private String generateSixDigitCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /** 본인 확인 결과. code는 컨트롤러가 세션에 저장해 이후 검증에 사용한다. */
    public record IdentifyResult(Status status, String userId, String code) {
        public enum Status { SUCCESS, NOT_FOUND, SEND_FAILED }

        public static IdentifyResult success(String userId, String code) {
            return new IdentifyResult(Status.SUCCESS, userId, code);
        }

        public static IdentifyResult notFound() {
            return new IdentifyResult(Status.NOT_FOUND, null, null);
        }

        public static IdentifyResult sendFailed() {
            return new IdentifyResult(Status.SEND_FAILED, null, null);
        }
    }
}
