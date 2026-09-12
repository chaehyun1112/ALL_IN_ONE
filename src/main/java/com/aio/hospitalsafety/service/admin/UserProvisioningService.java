// [CODEX 생성 파일] Backend A 계정 발급 및 상태 관리 작업을 위해 추가했습니다.
package com.aio.hospitalsafety.service.admin;

import com.aio.hospitalsafety.domain.AccountStatus;
import com.aio.hospitalsafety.domain.Role;
import com.aio.hospitalsafety.dto.admin.CreateUserRequest;
import com.aio.hospitalsafety.dto.admin.CreateUserResponse;
import com.aio.hospitalsafety.dto.admin.UserStatusResponse;
import com.aio.hospitalsafety.dto.admin.WardOptionResponse;
import com.aio.hospitalsafety.exception.UserConflictException;
import com.aio.hospitalsafety.exception.UserNotFoundException;
import com.aio.hospitalsafety.mapper.admin.UserProvisioningMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserProvisioningService {
    private final UserProvisioningMapper userProvisioningMapper;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final PasswordEncoder passwordEncoder;

    public UserProvisioningService(UserProvisioningMapper userProvisioningMapper,
                                   TemporaryPasswordGenerator temporaryPasswordGenerator,
                                   PasswordEncoder passwordEncoder) {
        this.userProvisioningMapper = userProvisioningMapper;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse createUser(String hospitalId, CreateUserRequest request) {
        String normalizedHospitalId = requireText(hospitalId, "병원 정보가 없습니다.");
        String normalizedUserId = requireText(request.userId(), "사용자 아이디를 입력해 주세요.");
        String normalizedUserName = requireText(request.userName(), "사용자 이름을 입력해 주세요.");

        if (request.role() != Role.USER) {
            throw new IllegalArgumentException("병원 관리자는 일반 사용자 계정만 발급할 수 있습니다.");
        }
        if (userProvisioningMapper.existsUserId(normalizedUserId)) {
            throw new UserConflictException("이미 사용 중인 사용자 아이디입니다.");
        }
        if (!userProvisioningMapper.existsWardInHospital(normalizedHospitalId, request.wardId())) {
            throw new IllegalArgumentException("현재 병원에 속한 병동을 선택해 주세요.");
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
        String passwordHash = passwordEncoder.encode(temporaryPassword);
        try {
            int inserted = userProvisioningMapper.insertUser(
                    normalizedUserId,
                    passwordHash,
                    normalizedHospitalId,
                    request.wardId(),
                    normalizedUserName,
                    request.role().name());
            if (inserted != 1) {
                throw new IllegalStateException("사용자 계정을 발급하지 못했습니다.");
            }
        } catch (DuplicateKeyException exception) {
            throw new UserConflictException("이미 사용 중인 사용자 아이디입니다.");
        }

        return new CreateUserResponse(
                normalizedUserId,
                normalizedUserName,
                request.wardId(),
                request.role(),
                AccountStatus.ACTIVE,
                true,
                temporaryPassword);
    }

    @Transactional
    public UserStatusResponse disableUser(String hospitalId, String userId) {
        String normalizedHospitalId = requireText(hospitalId, "병원 정보가 없습니다.");
        String normalizedUserId = requireText(userId, "사용자 아이디가 없습니다.");
        requireManagedUser(normalizedHospitalId, normalizedUserId);
        if (userProvisioningMapper.disableUser(normalizedHospitalId, normalizedUserId) != 1) {
            throw new UserConflictException("이미 비활성화되었거나 상태를 변경할 수 없는 계정입니다.");
        }
        return new UserStatusResponse(normalizedUserId, AccountStatus.DISABLED);
    }

    @Transactional
    public UserStatusResponse reactivateUser(String hospitalId, String userId) {
        String normalizedHospitalId = requireText(hospitalId, "병원 정보가 없습니다.");
        String normalizedUserId = requireText(userId, "사용자 아이디가 없습니다.");
        requireManagedUser(normalizedHospitalId, normalizedUserId);
        if (userProvisioningMapper.reactivateUser(normalizedHospitalId, normalizedUserId) != 1) {
            throw new UserConflictException("이미 활성화되었거나 상태를 변경할 수 없는 계정입니다.");
        }
        return new UserStatusResponse(normalizedUserId, AccountStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<WardOptionResponse> findWards(String hospitalId) {
        return userProvisioningMapper.findWardsByHospitalId(
                requireText(hospitalId, "병원 정보가 없습니다."));
    }

    private void requireManagedUser(String hospitalId, String userId) {
        if (!userProvisioningMapper.existsUserInHospital(hospitalId, userId)) {
            throw new UserNotFoundException("현재 병원에서 관리할 수 있는 사용자 계정이 없습니다.");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
