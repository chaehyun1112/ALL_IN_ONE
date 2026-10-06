package com.aio.hospitalsafety.service.admin;

import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * [2026.10.01 추가] 사고 영상 보관함 비밀번호 재확인.
 *
 * 관리자가 사고 영상 보관함을 열 때 현재 로그인한 관리자 계정의 비밀번호를 다시 입력하게 한다.
 * 맞으면 이 로그인 세션에서 UNLOCK_DURATION 동안 영상을 재생할 수 있다(AdminEventMediaController 가 확인).
 * 화면(accident-records.js)은 보관함을 열 때마다 비밀번호를 다시 묻는다. 서버는 영상 주소로 바로 들어오는 경우도 막는다.
 * 비밀번호를 MAX_FAILURES 번 틀리면 LOCK_DURATION 동안 확인을 받지 않는다(무차별 대입 방지).
 */
@Service
public class AccidentVideoAccessService {

    private static final Logger log = LoggerFactory.getLogger(AccidentVideoAccessService.class);

    // 세션에 남기는 값의 이름
    private static final String UNLOCKED_UNTIL = "accidentVideoUnlockedUntil";
    private static final String FAILED_COUNT = "accidentVideoFailedCount";
    private static final String LOCKED_UNTIL = "accidentVideoLockedUntil";

    // 비밀번호 확인 뒤 영상을 재생할 수 있는 시간
    private static final Duration UNLOCK_DURATION = Duration.ofMinutes(10);
    // 연속으로 틀릴 수 있는 횟수와, 넘었을 때 확인을 막는 시간
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    /** 비밀번호 확인 결과 */
    public enum UnlockResult {
        UNLOCKED,        // 맞음
        WRONG_PASSWORD,  // 틀림
        LOCKED           // 여러 번 틀려 잠시 확인을 막음
    }

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AccidentVideoAccessService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 현재 로그인한 관리자의 비밀번호와 같은지 확인한다. 맞으면 이 세션에서 영상 재생을 허용한다.
     */
    public UnlockResult unlock(HttpSession session, String hospitalId, String adminId, String password) {
        Instant now = Instant.now();
        Instant lockedUntil = (Instant) session.getAttribute(LOCKED_UNTIL);
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return UnlockResult.LOCKED;
        }

        String passwordHash = userMapper.findByHospitalIdAndUserId(hospitalId, adminId)
                .map(User::passwordHash)
                .orElse(null);
        boolean matched = password != null && !password.isEmpty()
                && passwordHash != null && passwordEncoder.matches(password, passwordHash);

        if (matched) {
            session.setAttribute(UNLOCKED_UNTIL, now.plus(UNLOCK_DURATION));
            session.removeAttribute(FAILED_COUNT);
            session.removeAttribute(LOCKED_UNTIL);
            return UnlockResult.UNLOCKED;
        }

        Integer failed = (Integer) session.getAttribute(FAILED_COUNT);
        int failedCount = (failed == null ? 0 : failed) + 1;
        if (failedCount >= MAX_FAILURES) {
            session.setAttribute(LOCKED_UNTIL, now.plus(LOCK_DURATION));
            session.removeAttribute(FAILED_COUNT);
            log.warn("사고 영상 보관함 비밀번호를 {}번 틀려 {}분 동안 확인을 막습니다. adminId={}",
                    MAX_FAILURES, LOCK_DURATION.toMinutes(), adminId);
            return UnlockResult.LOCKED;
        }
        session.setAttribute(FAILED_COUNT, failedCount);
        return UnlockResult.WRONG_PASSWORD;
    }

    /** 이 세션에서 비밀번호 확인을 마쳤고 아직 허용 시간이 남았으면 true */
    public boolean isUnlocked(HttpSession session) {
        if (session == null) {
            return false;
        }
        Instant unlockedUntil = (Instant) session.getAttribute(UNLOCKED_UNTIL);
        return unlockedUntil != null && unlockedUntil.isAfter(Instant.now());
    }

    /** 화면에 보여 줄 잠금 시간(분) */
    public long lockMinutes() {
        return LOCK_DURATION.toMinutes();
    }
}
