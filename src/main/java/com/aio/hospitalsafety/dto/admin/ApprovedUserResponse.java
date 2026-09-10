package com.aio.hospitalsafety.dto.admin;

public record ApprovedUserResponse(
        String userId,
        String userName,
        String authStatus,
        Long wardId,
        String wardName
) {
}