// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.mapper.HospitalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** 로그인 1단계에서 병원 구분 ID가 실제 등록된 병원인지 확인한다. */
@Service
public class HospitalService {

    private final HospitalMapper hospitalMapper;

    public HospitalService(HospitalMapper hospitalMapper) {
        this.hospitalMapper = hospitalMapper;
    }

    /** 조회만 수행하므로 readOnly 트랜잭션을 사용한다. */
    @Transactional(readOnly = true)
    public Optional<Hospital> findRegisteredHospital(String hospitalId) {
        return hospitalMapper.findByHospitalId(hospitalId.trim());
    }
}
