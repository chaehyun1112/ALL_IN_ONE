// PGH
package com.aio.hospitalsafety.config;

import com.aio.hospitalsafety.domain.User;
import org.springframework.security.core.authority.AuthorityUtils;

/** 인증된 직원의 소속 병원을 로그인 정보와 함께 보관한다. */
public class HospitalUserDetails extends org.springframework.security.core.userdetails.User {
    private static final long serialVersionUID = 1L;
    private final String hospitalId;

    public HospitalUserDetails(User user) {
        super(user.userId(), user.passwordHash(), AuthorityUtils.createAuthorityList(
                "ROLE_" + user.role().name(), "STATUS_" + user.approvalStatus().name()));
        this.hospitalId = user.hospitalId();
    }

    public String getHospitalId() {
        return hospitalId;
    }
}
