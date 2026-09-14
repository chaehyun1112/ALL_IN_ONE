package com.aio.hospitalsafety.mapper.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProvisioningMapper {

    boolean existsUserId(
            @Param("userId") String userId
    );

    boolean existsWardInHospital(
            @Param("hospitalId") String hospitalId,
            @Param("wardId") Long wardId
    );

    int insertUser(
            @Param("userId") String userId,
            @Param("passwordHash") String passwordHash,
            @Param("hospitalId") String hospitalId,
            @Param("wardId") Long wardId,
            @Param("userName") String userName
    );
}