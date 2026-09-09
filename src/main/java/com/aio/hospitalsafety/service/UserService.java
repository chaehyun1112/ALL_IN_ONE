// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직원 계정과 관련된 비즈니스 규칙을 처리하는 Service 계층이다.
 *
 * MVC Layered Architecture에서 각 계층의 역할
 * - Controller: HTTP 요청/응답과 화면 이동
 * - Service: 현재 PW 확인, 해시 생성 같은 업무 규칙
 * - Mapper: SQL 실행과 DB 접근
 *
 * @Service를 붙이면 Spring이 이 클래스를 Bean으로 생성하므로 Controller에서 주입받을 수 있다.
 */
@Service
public class UserService {

    // TB_EMP 조회와 UPDATE SQL을 실행하는 MyBatis Mapper
    private final UserMapper userMapper;
    // SecurityConfig에서 Bean으로 등록한 BCryptPasswordEncoder
    private final PasswordEncoder passwordEncoder;

    /** 필요한 객체를 생성자로 주입받는다. 테스트에서는 Mock 객체를 넣기도 쉽다. */
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 로그인한 직원의 현재 비밀번호를 확인하고 새 비밀번호로 변경한다.
     *
     * @param hospitalId 로그인 1단계에서 Session에 저장한 병원 구분 ID
     * @param userId 로그인 Session에서 얻은 일반 사용자의 직원 ID(EMP_ID)
     * @param currentPassword 사용자가 입력한 현재 PW 원문. 저장하거나 로그로 출력하면 안 된다.
     * @param newPassword 사용자가 입력한 새 PW 원문. encode 직후 더 이상 사용하지 않는다.
     * @return Controller가 화면 흐름을 결정할 수 있도록 PasswordChangeResult를 반환한다.
     */
    // RuntimeException이 발생하면 이 메서드 안에서 실행한 DB 변경을 롤백한다.
    @Transactional
    public PasswordChangeResult changePassword(
            String hospitalId, String userId, String currentPassword, String newPassword) {
        // Optional.orElse(null): 조회 결과가 없으면 null로 바꿔 아래 조건문에서 처리한다.
        User user = userMapper.findByHospitalIdAndUserId(hospitalId, userId).orElse(null);
        if (user == null) {
            return PasswordChangeResult.USER_NOT_FOUND;
        }

        // matches(원문, 저장된 해시)로 비교한다. BCrypt 해시를 복호화하는 코드는 존재하지 않는다.
        // equals로 두 문자열을 비교하면 안 된다. 같은 PW도 BCrypt encode 결과는 매번 달라지기 때문이다.
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            return PasswordChangeResult.CURRENT_PASSWORD_MISMATCH;
        }

        // DB에는 새 비밀번호 원문이 아닌 BCrypt 해시만 전달한다.
        String newPasswordHash = passwordEncoder.encode(newPassword);

        // MyBatis update 메서드는 수정된 행 수를 반환한다.
        // EMP_ID는 PK이므로 정상적으로 수정되면 반드시 1이어야 한다.
        if (userMapper.updatePassword(user.userId(), newPasswordHash) != 1) {
            // 예외를 던지면 @Transactional이 DB 작업을 롤백한다.
            throw new IllegalStateException("비밀번호 변경에 실패했습니다.");
        }
        return PasswordChangeResult.SUCCESS;
    }

    /** 문자열이나 숫자 대신 enum을 사용해 가능한 처리 결과를 명확하게 제한한다. */
    public enum PasswordChangeResult {
        SUCCESS,                  // 변경 성공
        CURRENT_PASSWORD_MISMATCH, // 사용자가 입력한 현재 PW가 DB 해시와 불일치
        USER_NOT_FOUND            // Session의 직원 ID에 해당하는 TB_EMP 행이 없음
    }

    /**
     * 로그인 전 상태에서 직원 ID + 이름 일치를 확인한 뒤 비밀번호를 재설정한다.
     *
     * 본인 확인 수단이 직원 ID/이름뿐이라 보안 수준이 낮다. 아이디가 아예 없는 경우와
     * 이름만 틀린 경우를 IDENTITY_MISMATCH 하나로 묶어서, 오류 메시지만 보고
     * "이 아이디가 실제로 존재하는지"를 추측할 수 없게 한다.
     *
     * @param hospitalId 로그인 1단계(AuthController)에서 Session에 저장한 병원 구분 ID
     * @param employeeId 사용자가 입력한 직원 ID
     * @param employeeName 사용자가 입력한 이름. TB_EMP.EMP_NM과 정확히 같아야 한다.
     * @param newPassword 새 비밀번호 원문. encode 직후 더 이상 사용하지 않는다.
     */
    @Transactional
    public PasswordResetResult resetPassword(
            String hospitalId, String employeeId, String employeeName, String newPassword) {
        User user = userMapper.findByHospitalIdAndUserId(hospitalId, employeeId).orElse(null);
        if (user == null || !user.userName().equals(employeeName)) {
            return PasswordResetResult.IDENTITY_MISMATCH;
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        if (userMapper.updatePassword(user.userId(), newPasswordHash) != 1) {
            throw new IllegalStateException("비밀번호 재설정에 실패했습니다.");
        }
        return PasswordResetResult.SUCCESS;
    }

    public enum PasswordResetResult {
        SUCCESS,
        IDENTITY_MISMATCH // 아이디가 없거나 이름이 일치하지 않음(둘을 구분해서 알려주지 않는다)
    }
}
