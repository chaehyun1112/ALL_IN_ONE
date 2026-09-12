package com.aio.hospitalsafety.mapper.admin;

import com.aio.hospitalsafety.dto.admin.WardOptionResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserProvisioningMapper {
    boolean existsUserId(@Param("userId") String userId);

    boolean existsUserInHospital(@Param("hospitalId") String hospitalId,
                                 @Param("userId") String userId);

    boolean existsWardInHospital(@Param("hospitalId") String hospitalId,
                                 @Param("wardId") Long wardId);

    int insertUser(@Param("userId") String userId,
                   @Param("passwordHash") String passwordHash,
                   @Param("hospitalId") String hospitalId,
                   @Param("wardId") Long wardId,
                   @Param("userName") String userName,
                   @Param("role") String role);

    int disableUser(@Param("hospitalId") String hospitalId,
                    @Param("userId") String userId);

    int reactivateUser(@Param("hospitalId") String hospitalId,
                       @Param("userId") String userId);

    List<WardOptionResponse> findWardsByHospitalId(@Param("hospitalId") String hospitalId);
}
