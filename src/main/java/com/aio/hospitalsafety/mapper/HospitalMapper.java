// PGH
package com.aio.hospitalsafety.mapper;

import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.dto.HospitalDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 실제 TB_HOSPITAL을 조회하는 MyBatis Mapper다.
 *
 * 병합(park + chae) 과정에서 두 흐름이 각자 만든 조회 메서드가 남아 있다.
 * - findByHospitalId: 2단계 로그인(AuthController)에서 사용, HospitalMapper.xml에 매핑됨
 * - findHospitalByDomain: 도메인 선택/회원가입(HomeController, SignupService)에서 사용, 어노테이션 매핑
 * 두 메서드 모두 결국 TB_HOSPITAL을 같은 키(HOSP_DIV_ID)로 조회하는 동일한 기능이라
 * 추후 하나로 통합하는 것이 좋지만, 이번 병합에서는 두 흐름을 모두 그대로 동작시키기 위해 남겨둔다.
 */
@Mapper
public interface HospitalMapper {

    /** HOSP_DIV_ID로 병원을 조회한다. 등록되지 않은 ID이면 Optional.empty()를 반환한다. */
    Optional<Hospital> findByHospitalId(String hospitalId);

    @Select("SELECT hosp_div_id AS hospital_domain, hosp_nm AS hospital_name "
            + "FROM public.tb_hospital WHERE hosp_div_id = #{hospitalDomain}")
    HospitalDto findHospitalByDomain(@Param("hospitalDomain") String hospitalDomain);
}
