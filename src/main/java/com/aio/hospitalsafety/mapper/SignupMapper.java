package com.aio.hospitalsafety.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aio.hospitalsafety.dto.WardOption;

@Mapper
public interface SignupMapper {

    // 병원 존재 여부 확인
    boolean existsHospital(
            @Param("hospDivId") String hospDivId
    );

    // 서비스 전체에서 사용자 아이디 중복 확인
    boolean existsUserId(
            @Param("userId") String userId
    );

    // 선택한 병동이 현재 병원 소속인지 확인
    boolean existsWardInHospital(
            @Param("hospDivId") String hospDivId,
            @Param("wardId") Long wardId
    );

    // 현재 병원에 속한 병동 목록 조회
    List<WardOption> findWardsByHospital(
            @Param("hospDivId") String hospDivId
    );

    // 사용자 회원가입 정보 저장
    int insertUser(
            @Param("userId") String userId,
            @Param("encodedPassword") String encodedPassword,
            @Param("userName") String userName,
            @Param("email") String email,
            @Param("hospDivId") String hospDivId,
            @Param("wardId") Long wardId
    );
}