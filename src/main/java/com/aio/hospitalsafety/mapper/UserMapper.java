// PGH
package com.aio.hospitalsafety.mapper;

import com.aio.hospitalsafety.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * TB_EMP 테이블에 접근하는 MyBatis Mapper 인터페이스다.
 *
 * @Mapper가 붙으면 MyBatis가 실행 시점에 구현 객체를 자동 생성한다.
 * 개발자가 구현 클래스를 직접 만들지 않아도 된다.
 * 메서드명은 resources/mapper/UserMapper.xml의 select/update id와 같아야 한다.
 */
@Mapper
public interface UserMapper {

    /**
     * 최초 로그인 비밀번호 변경이 필요한 계정인지 조회한다.
     */
    Boolean isInitialUserPassword(
            @Param("hospitalId") String hospitalId,
            @Param("userId") String userId);

    /**
     * 로그인 또는 계정 확인 시 HOSP_DIV_ID와 EMP_ID가 모두 일치하는 직원 한 명을 조회한다.
     * XML 연결: &lt;select id="findByHospitalIdAndUserId"&gt;
     * Optional은 조회 결과가 없을 수 있다는 사실을 반환 타입으로 표현한다.
     */
    Optional<User> findByHospitalIdAndUserId(
            @Param("hospitalId") String hospitalId,
            @Param("userId") String userId);

    /**
     * EMP_ID가 일치하는 직원의 EMP_PW와 UPD_DT를 변경한다.
     * 비밀번호 변경이 완료되면 MUST_CHANGE_PASSWORD를 FALSE로 변경한다.
     */
    int updatePassword(
            @Param("userId") String userId,
            @Param("passwordHash") String passwordHash);

    /**
     * 로그인 실패 시 FAILED_LOGIN_COUNT를 1 올리고, 5회 단위로 걸릴 때마다
     * LOCKED_UNTIL을 현재로부터 1분 뒤로 새로 건다(브루트포스 방어).
     *
     * @return 갱신된 FAILED_LOGIN_COUNT. 대상 계정이 없으면(도메인/아이디 불일치) null.
     */
    Integer registerFailedLogin(
            @Param("hospitalId") String hospitalId,
            @Param("userId") String userId);

    /**
     * 로그인 성공 시 실패 카운트와 잠금을 초기화한다.
     */
    int resetFailedLogin(
            @Param("hospitalId") String hospitalId,
            @Param("userId") String userId);

    /**
     * 로그인 실패(FAIL) 또는 그로 인한 잠금 발생(LOCKED)을 이력에 남긴다.
     */
    void recordLoginHistory(
            @Param("hospitalId") String hospitalId,
            @Param("userId") String userId,
            @Param("eventCode") String eventCode);
}