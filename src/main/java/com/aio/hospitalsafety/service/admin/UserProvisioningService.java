package com.aio.hospitalsafety.service.admin;

import com.aio.hospitalsafety.domain.ApprovalStatus;
import com.aio.hospitalsafety.domain.Role;
import com.aio.hospitalsafety.dto.admin.CreateUserRequest;
import com.aio.hospitalsafety.dto.admin.CreateCaregiverRequest;
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
    private final AdminHistoryService adminHistoryService;

    public UserProvisioningService(
            UserProvisioningMapper userProvisioningMapper,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            PasswordEncoder passwordEncoder,
            AdminHistoryService adminHistoryService
    ) {
        this.userProvisioningMapper = userProvisioningMapper;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.adminHistoryService = adminHistoryService;
    }

    /**
     * 관리자가 현재 병원에 직원 계정을 생성한다.
     *
     * 계정 생성과 감사 로그 저장은 같은 트랜잭션에서 처리한다.
     * 감사 로그 저장에 실패하면 계정 생성도 함께 취소된다.
     */
    @Transactional
    public CreateUserResponse createUser(
            String hospitalId,
            String adminId,
            CreateUserRequest request,
            String jobType
    ) {
        // [2026.09.27] 간호사 번호도 간병인처럼 하이픈을 빼고 저장한다(확정 낙상 SMS 받는 번호).
        String phoneNumber = request.phoneNumber().replace("-", "");
        return createAccount(hospitalId, adminId, request.userId(), request.userName(),
                request.wardId(), jobType, phoneNumber, null);
    }

    @Transactional
    public CreateUserResponse createCaregiver(
            String hospitalId,
            String adminId,
            CreateCaregiverRequest request
    ) {
        String phoneNumber = request.phoneNumber().replace("-", "");
        String userId = "cg." + phoneNumber;
        // [2026.09.27] 번호를 바꾼 예전 간병인이 이 번호로 만든 아이디(cg.번호)를 이미 쓰고 있으면
        // 뒤에 -2, -3 … 을 붙여 빈 아이디를 쓴다. (지금 이 번호를 쓰는 직원이 있는지는 createAccount 가 따로 막는다)
        for (int suffix = 2; suffix <= 9 && userProvisioningMapper.existsUserId(userId); suffix++) {
            userId = "cg." + phoneNumber + "-" + suffix;
        }
        Long wardId = userProvisioningMapper.findThirdFloorWardId(hospitalId);
        if (wardId == null) {
            throw new IllegalArgumentException("현재 병원에 3병동이 등록되어 있지 않습니다.");
        }
        return createAccount(hospitalId, adminId, userId, request.userName(),
                wardId, "CAREGIVER", phoneNumber, String.valueOf(request.roomNumber()));
    }

    private CreateUserResponse createAccount(
            String hospitalId,
            String adminId,
            String userId,
            String userName,
            Long wardId,
            String jobType,
            String phoneNumber,
            String roomNumber
    ) {
        String normalizedHospitalId = requireText(
                hospitalId,
                "병원 정보가 없습니다."
        );

        String normalizedAdminId = requireText(
                adminId,
                "작업 관리자 정보가 없습니다."
        );

        String normalizedUserId = requireText(
                userId,
                "직원 아이디를 입력해 주세요."
        );

        String normalizedUserName = requireText(
                userName,
                "직원 이름을 입력해 주세요."
        );

        // [2026.09.27] 같은 번호를 쓰는 활성 직원이 있으면 막는다(전화번호 수정과 같은 규칙, 한 번호로 SMS 두 번 방지).
        if (phoneNumber != null
                && userProvisioningMapper.existsActivePhone(normalizedHospitalId, phoneNumber)) {
            throw new UserConflictException(
                    "다른 직원이 이미 쓰는 전화번호입니다."
            );
        }

        if (userProvisioningMapper.existsUserId(normalizedUserId)) {
            throw new UserConflictException(
                    "이미 사용 중인 직원 아이디입니다."
            );
        }

        if (!userProvisioningMapper.existsWardInHospital(
                normalizedHospitalId,
                wardId
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
                    wardId,
                    normalizedUserName,
                    jobType,
                    phoneNumber,
                    roomNumber
            );

            if (inserted != 1) {
                throw new IllegalStateException(
                        "직원 계정을 생성하지 못했습니다."
                );
            }
        } catch (DuplicateKeyException exception) {
            // [2026.09.27] 간병인은 번호에 DB 고유 인덱스(ux_emp_caregiver_phone)가 있어 비활성 간병인과 번호가 같아도 여기서 막힌다.
            throw new UserConflictException(
                    "CAREGIVER".equals(jobType)
                            ? "이미 등록된 전화번호입니다. 비활성화된 간병인 목록도 확인해 주세요."
                            : "이미 사용 중인 직원 아이디입니다."
            );
        }

        // 직원 계정 생성에 성공한 경우에만 CREATE 감사 로그를 저장한다.
        adminHistoryService.record(
                normalizedHospitalId,
                normalizedAdminId,
                normalizedUserId,
                AdminHistoryService.CREATE
        );

        return new CreateUserResponse(
                normalizedUserId,
                normalizedUserName,
                wardId,
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
