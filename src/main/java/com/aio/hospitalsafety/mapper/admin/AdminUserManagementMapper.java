package com.aio.hospitalsafety.mapper.admin;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aio.hospitalsafety.dto.WardOption;
import com.aio.hospitalsafety.dto.admin.ApprovedUserResponse;
import com.aio.hospitalsafety.dto.admin.InactiveUserResponse;

@Mapper
public interface AdminUserManagementMapper {

    // 승인 완료 사용자 목록 조회
    List<ApprovedUserResponse> findApprovedUsers(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("keyword") String keyword,
            @Param("jobType") String jobType
    );

    // 현재 병원의 병동 목록 조회
    List<WardOption> findWardsByHospital(
            @Param("hospitalDomain") String hospitalDomain
    );

    // 선택한 병동이 현재 병원 소속인지 확인
    boolean existsWardInHospital(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("wardId") Long wardId
    );

    // 승인 완료 사용자의 담당 병동 변경
    int updateUserWard(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("userId") String userId,
            @Param("wardId") Long wardId
    );

    // 비활성화된 일반 사용자 목록 조회
    List<InactiveUserResponse> findInactiveUsers(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("jobType") String jobType
    );

    // 비활성화된 일반 사용자 계정 재활성화
    int activateUser(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("userId") String userId
    );

    // 비활성화된 일반 사용자 계정 영구 삭제
    int deleteInactiveUser(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("userId") String userId
    );

    // 계정 비활성화: APPROVED에서 INACTIVE로 변경
    int deactivateUser(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("userId") String userId
    );

    // 활성 직원의 비밀번호를 임시 비밀번호로 초기화하고 최초 변경 필요 상태로 변경
    int resetUserPassword(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("userId") String userId,
            @Param("passwordHash") String passwordHash
    );
}
