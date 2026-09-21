package com.aio.hospitalsafety.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aio.hospitalsafety.dto.WardOption;
import com.aio.hospitalsafety.dto.admin.ApprovedUserResponse;
import com.aio.hospitalsafety.dto.admin.InactiveUserResponse;
import com.aio.hospitalsafety.mapper.admin.AdminUserManagementMapper;

@Service
public class AdminUserManagementService {

    private final AdminUserManagementMapper adminUserManagementMapper;
    private final AdminHistoryService adminHistoryService;

    public AdminUserManagementService(
            AdminUserManagementMapper adminUserManagementMapper,
            AdminHistoryService adminHistoryService
    ) {
        this.adminUserManagementMapper = adminUserManagementMapper;
        this.adminHistoryService = adminHistoryService;
    }

    // 승인 완료 사용자 목록 조회
    @Transactional(readOnly = true)
    public List<ApprovedUserResponse> getApprovedUsers(
            String hospitalDomain,
            String keyword,
            String jobType
    ) {
        String normalizedKeyword =
                keyword == null ? "" : keyword.trim();

        return adminUserManagementMapper.findApprovedUsers(
                hospitalDomain,
                normalizedKeyword,
                jobType
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
            String adminId,
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

        // 담당 병동 변경 성공 이력을 같은 트랜잭션으로 저장한다.
        adminHistoryService.record(
                hospitalDomain,
                adminId,
                userId,
                AdminHistoryService.CHANGE_WARD
        );
    }

    // 비활성화된 일반 사용자 목록 조회
    @Transactional(readOnly = true)
    public List<InactiveUserResponse> getInactiveUsers(
            String hospitalDomain,
            String jobType
    ) {
        return adminUserManagementMapper.findInactiveUsers(
                hospitalDomain,
                jobType
        );
    }

    // 비활성화된 일반 사용자 계정 재활성화
    @Transactional
    public void activateUser(
            String hospitalDomain,
            String adminId,
            String userId
    ) {
        int updatedRows =
                adminUserManagementMapper.activateUser(
                        hospitalDomain,
                        userId
                );

        if (updatedRows != 1) {
            throw new IllegalArgumentException(
                    "활성화할 비활성화 사용자를 찾을 수 없습니다. 목록을 새로고침해 주세요."
            );
        }

        // 계정 재활성화 성공 이력을 같은 트랜잭션으로 저장한다.
        adminHistoryService.record(
                hospitalDomain,
                adminId,
                userId,
                AdminHistoryService.ACTIVATE
        );
    }

    // 비활성화된 일반 사용자 계정 영구 삭제
    @Transactional
    public void deleteInactiveUser(
            String hospitalDomain,
            String adminId,
            String userId
    ) {
        int deletedRows =
                adminUserManagementMapper.deleteInactiveUser(
                        hospitalDomain,
                        userId
                );

        if (deletedRows != 1) {
            throw new IllegalArgumentException(
                    "삭제할 비활성화 사용자를 찾을 수 없습니다. 목록을 새로고침해 주세요."
            );
        }

        // 직원 삭제 후에도 감사 테이블에 직원 ID를 보존한다.
        adminHistoryService.record(
                hospitalDomain,
                adminId,
                userId,
                AdminHistoryService.DELETE
        );
    }

    // 계정 비활성화: APPROVED에서 INACTIVE로 변경
    @Transactional
    public void deactivateUser(
            String hospitalDomain,
            String adminId,
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

        // 계정 비활성화 성공 이력을 같은 트랜잭션으로 저장한다.
        adminHistoryService.record(
                hospitalDomain,
                adminId,
                userId,
                AdminHistoryService.DEACTIVATE
        );
    }
}
