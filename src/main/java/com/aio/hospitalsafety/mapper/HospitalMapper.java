// PGH
package com.aio.hospitalsafety.mapper;

import com.aio.hospitalsafety.domain.Hospital;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/** 실제 TB_HOSPITAL을 조회하는 MyBatis Mapper다. */
@Mapper
public interface HospitalMapper {

    /** HOSP_DIV_ID로 병원을 조회한다. 등록되지 않은 ID이면 Optional.empty()를 반환한다. */
    Optional<Hospital> findByHospitalId(String hospitalId);
}
