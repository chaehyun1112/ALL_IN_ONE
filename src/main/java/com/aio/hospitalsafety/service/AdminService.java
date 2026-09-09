package com.aio.hospitalsafety.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aio.hospitalsafety.dto.UserDto;
import com.aio.hospitalsafety.mapper.AdminMapper;

@Service
public class AdminService {

    private final AdminMapper adminMapper;

    public AdminService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findUsers(String hospitalDomain) {
        return adminMapper.findUsersByHospital(hospitalDomain);
    }

    @Transactional
    public boolean approveUser(String hospitalDomain, String userId) {
        return adminMapper.approveUser(hospitalDomain, userId) == 1;
    }

    @Transactional
    public boolean rejectUser(String hospitalDomain, String userId) {
        return adminMapper.rejectUser(hospitalDomain, userId) == 1;
    }
}
