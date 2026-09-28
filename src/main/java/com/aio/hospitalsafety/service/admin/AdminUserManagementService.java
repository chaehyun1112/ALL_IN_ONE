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

    /**
     * [2026.09.27] 간병인 담당 병실 변경.
     * 확정 낙상 SMS 는 병실 번호(room_no)로 담당 간병인을 찾으므로, 화면에서만 바꾸면 문자가 예전 간병인에게 간다.
     * 병실 번호의 백의 자리가 병동 번호다(305호 → 3병동). 병동도 함께 바꾼다.
     * 이력은 CHANGE_WARD 로 남긴다. 간병인 이력 화면은 이 코드를 '병실 변경'으로 보여 준다(admin-history.js).
     */
    @Transactional
    public void changeCaregiverRoom(
            String hospitalDomain,
            String adminId,
            String userId,
            int roomNumber
    ) {
        int wardNumber = roomNumber / 100;
        int roomOrder = roomNumber % 100;
        if (wardNumber < 1 || wardNumber > 3 || roomOrder < 1 || roomOrder > 17) {
            throw new IllegalArgumentException("101~117호, 201~217호, 301~317호 중에서 선택해 주세요.");
        }

        Long wardId = adminUserManagementMapper.findWardIdByNumber(hospitalDomain, wardNumber);
        if (wardId == null) {
            throw new IllegalArgumentException("현재 병원에 " + wardNumber + "병동이 없습니다.");
        }

        int updatedRows = adminUserManagementMapper.updateCaregiverRoom(
                hospitalDomain, userId, wardId, String.valueOf(roomNumber));
        if (updatedRows != 1) {
            throw new IllegalArgumentException("병실을 변경할 간병인을 찾을 수 없습니다.");
        }

        adminHistoryService.record(hospitalDomain, adminId, userId, AdminHistoryService.CHANGE_WARD);
    }

    /**
     * [2026.09.27] 간호사·간병인 전화번호 수정 (확정 낙상 SMS 받는 번호).
     * 같은 번호를 쓰는 다른 활성 직원이 있으면 막는다(한 번호로 문자가 두 번 가지 않게).
     * 관리 이력은 남기지 않는다. 공용 DB 의 ck_admin_history_action 이 작업 코드 6개만 허용해서,
     * 전화번호 수정 코드를 넣으려면 DB 제약부터 바꿔야 한다(팀 결정 필요).
     */
    @Transactional
    public void changeUserPhone(
            String hospitalDomain,
            String userId,
            String phoneNumber
    ) {
        String digits = phoneNumber.replace("-", "");
        if (adminUserManagementMapper.existsOtherUserPhone(hospitalDomain, userId, digits)) {
            throw new IllegalArgumentException("다른 직원이 이미 쓰는 전화번호입니다.");
        }

        int updatedRows = adminUserManagementMapper.updateUserPhone(hospitalDomain, userId, digits);
        if (updatedRows != 1) {
            throw new IllegalArgumentException("전화번호를 바꿀 직원을 찾을 수 없습니다.");
        }
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
        // [2026.09.27] 비활성인 동안 같은 번호로 새 직원을 만들었다면, 다시 활성화할 때 같은 번호의 활성 직원이 둘이 된다
        // (같은 휴대폰에 확정 낙상 문자가 두 번 감). 생성·번호 수정과 같은 규칙으로 막는다.
        String phoneNumber = adminUserManagementMapper.findUserPhone(hospitalDomain, userId);
        if (phoneNumber != null && !phoneNumber.isBlank()
                && adminUserManagementMapper.existsOtherUserPhone(hospitalDomain, userId, phoneNumber)) {
            throw new IllegalArgumentException("같은 전화번호를 쓰는 활성 직원이 있습니다. 번호를 먼저 바꿔 주세요.");
        }

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
