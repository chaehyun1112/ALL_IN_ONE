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
}