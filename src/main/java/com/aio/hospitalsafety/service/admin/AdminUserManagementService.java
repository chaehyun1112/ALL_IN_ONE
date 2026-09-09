package com.aio.hospitalsafety.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aio.hospitalsafety.dto.WardOption;
import com.aio.hospitalsafety.dto.admin.ApprovedUserResponse;
import com.aio.hospitalsafety.mapper.admin.AdminUserManagementMapper;

@Service
public class AdminUserManagementService {

    private final AdminUserManagementMapper adminUserManagementMapper;

    public AdminUserManagementService(
            AdminUserManagementMapper adminUserManagementMapper
    ) {
        this.adminUserManagementMapper = adminUserManagementMapper;
    }

    // 승인 완료 사용자 목록 조회
    @Transactional(readOnly = true)
    public List<ApprovedUserResponse> getApprovedUsers(
            String hospitalDomain,
            String keyword
    ) {
        String normalizedKeyword =
                keyword == null ? "" : keyword.trim();

        return adminUserManagementMapper.findApprovedUsers(
                hospitalDomain,
                normalizedKeyword
        );
    }

    // 현재 병원의 병동 목록 조회
    @Transactional(readOnly = true)
    public List<WardOption> getWards(
            String hospitalDomain
    ) {
        return adminUserManagementMapper.findWardsByHospital(
                hospitalDomain
        );
    }

    // 승인 완료 사용자의 담당 병동 변경
    @Transactional
    public void changeUserWard(
            String hospitalDomain,
            String userId,
            Long wardId
    ) {
        boolean wardExists =
                adminUserManagementMapper.existsWardInHospital(
                        hospitalDomain,
                        wardId
                );

        if (!wardExists) {
            throw new IllegalArgumentException(
                    "현재 병원에 속한 병동을 선택해 주세요."
            );
        }

        int updatedRows =
                adminUserManagementMapper.updateUserWard(
                        hospitalDomain,
                        userId,
                        wardId
                );

        if (updatedRows != 1) {
            throw new IllegalArgumentException(
                    "병동을 변경할 승인 완료 사용자를 찾을 수 없습니다."
            );
        }
    }

    // 계정 비활성화: APPROVED에서 PENDING으로 변경
    @Transactional
    public void deactivateUser(
            String hospitalDomain,
            String userId
    ) {
        int updatedRows =
                adminUserManagementMapper.deactivateUser(
                        hospitalDomain,
                        userId
                );

        if (updatedRows != 1) {
            throw new IllegalArgumentException(
                    "비활성화할 승인 완료 사용자를 찾을 수 없습니다."
            );
        }
    }
}