package com.aio.hospitalsafety.mapper.admin;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aio.hospitalsafety.dto.WardOption;
import com.aio.hospitalsafety.dto.admin.ApprovedUserResponse;

@Mapper
public interface AdminUserManagementMapper {

    // 승인 완료 사용자 목록 조회
    List<ApprovedUserResponse> findApprovedUsers(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("keyword") String keyword
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

    // 계정 비활성화: APPROVED에서 PENDING으로 변경
    int deactivateUser(
            @Param("hospitalDomain") String hospitalDomain,
            @Param("userId") String userId
    );
}