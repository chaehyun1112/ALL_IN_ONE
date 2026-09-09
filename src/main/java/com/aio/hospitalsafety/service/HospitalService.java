// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.domain.Hospital;
import com.aio.hospitalsafety.dto.HospitalDto;
import com.aio.hospitalsafety.mapper.HospitalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * TB_HOSPITAL 조회를 담당한다.
 *
 * findRegisteredHospital: 2단계 로그인(AuthController)에서 사용.
 * findHospitalByDomain: 도메인 선택/회원가입(HomeController, SignupService)에서 사용.
 * 두 메서드 모두 결국 같은 테이블을 조회하므로 추후 하나로 통합하는 것이 좋다.
 */
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

    public HospitalDto findHospitalByDomain(String hospitalDomain) {
        if (hospitalDomain == null || hospitalDomain.isBlank()) {
            return null;
        }
        return hospitalMapper.findHospitalByDomain(hospitalDomain.strip());
    }
}
