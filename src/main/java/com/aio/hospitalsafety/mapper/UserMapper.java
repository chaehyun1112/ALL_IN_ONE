// PGH
package com.aio.hospitalsafety.mapper;

import com.aio.hospitalsafety.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
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
     * 로그인 또는 계정 확인 시 HOSP_DIV_ID와 EMP_ID가 모두 일치하는 직원 한 명을 조회한다.
     * XML 연결: &lt;select id="findByHospitalIdAndUserId"&gt;
     * Optional은 조회 결과가 없을 수 있다는 사실을 반환 타입으로 표현한다.
     */
    Optional<User> findByHospitalIdAndUserId(
            @Param("hospitalId") String hospitalId,
            @Param("userId") String userId);

    /**
     * EMP_ID가 일치하는 직원의 EMP_PW와 UPD_DT를 변경한다.
     * @Param 이름은 XML 안의 #{userId}, #{passwordHash}와 연결된다.
     * 반환값은 UPDATE된 행의 개수이며 정상적으로 한 명이 수정되면 1이다.
     */
    int updatePassword(@Param("userId") String userId, @Param("passwordHash") String passwordHash);

    /**
     * 아이디 찾기 1단계 본인 확인용 조회다. HOSP_DIV_ID + 이름 + 이메일이 모두 일치하는
     * 직원 ID 목록을 반환한다. 정상적인 데이터라면 0건(불일치) 또는 1건(일치)이어야 하며,
     * Service에서 정확히 1건일 때만 인증코드를 발송한다(2건 이상이면 모호하므로 실패로 처리).
     */
    List<String> findMatchingUserIdsForFindId(
            @Param("hospitalId") String hospitalId,
            @Param("userName") String userName,
            @Param("email") String email);

    /**
     * 비밀번호 재설정 화면의 '아이디 확인' 버튼용 조회다. HOSP_DIV_ID + EMP_ID + EMP_NM이
     * 모두 일치하는지만 boolean으로 반환한다. 아이디만으로 존재 여부를 알려주면 계정 열거
     * 공격에 악용될 수 있어, 이름까지 함께 일치해야 확인되도록 한다.
     */
    boolean existsByHospitalIdAndUserIdAndUserName(
            @Param("hospitalId") String hospitalId,
            @Param("userId") String userId,
            @Param("userName") String userName);
}
