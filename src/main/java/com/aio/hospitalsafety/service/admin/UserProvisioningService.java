package com.aio.hospitalsafety.service.admin;

import com.aio.hospitalsafety.domain.ApprovalStatus;
import com.aio.hospitalsafety.domain.Role;
import com.aio.hospitalsafety.dto.admin.CreateUserRequest;
import com.aio.hospitalsafety.dto.admin.CreateUserResponse;
import com.aio.hospitalsafety.exception.UserConflictException;
import com.aio.hospitalsafety.mapper.admin.UserProvisioningMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningService {

    private final UserProvisioningMapper userProvisioningMapper;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final PasswordEncoder passwordEncoder;

    public UserProvisioningService(
            UserProvisioningMapper userProvisioningMapper,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.userProvisioningMapper = userProvisioningMapper;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse createUser(
            String hospitalId,
            CreateUserRequest request
    ) {
        String normalizedHospitalId = requireText(
                hospitalId,
                "병원 정보가 없습니다."
        );

        String normalizedUserId = requireText(
                request.userId(),
                "직원 아이디를 입력해 주세요."
        );

        String normalizedUserName = requireText(
                request.userName(),
                "직원 이름을 입력해 주세요."
        );

        if (userProvisioningMapper.existsUserId(normalizedUserId)) {
            throw new UserConflictException(
                    "이미 사용 중인 직원 아이디입니다."
            );
        }

        if (!userProvisioningMapper.existsWardInHospital(
                normalizedHospitalId,
                request.wardId()
        )) {
            throw new IllegalArgumentException(
                    "현재 병원에 속한 병동을 선택해 주세요."
            );
        }

        String temporaryPassword =
                temporaryPasswordGenerator.generate();

        String passwordHash =
                passwordEncoder.encode(temporaryPassword);

        try {
            int inserted = userProvisioningMapper.insertUser(
                    normalizedUserId,
                    passwordHash,
                    normalizedHospitalId,
                    request.wardId(),
                    normalizedUserName
            );

            if (inserted != 1) {
                throw new IllegalStateException(
                        "직원 계정을 생성하지 못했습니다."
                );
            }
        } catch (DuplicateKeyException exception) {
            throw new UserConflictException(
                    "이미 사용 중인 직원 아이디입니다."
            );
        }

        return new CreateUserResponse(
                normalizedUserId,
                normalizedUserName,
                request.wardId(),
                Role.USER,
                ApprovalStatus.APPROVED,
                true,
                temporaryPassword
        );
    }

    private String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}