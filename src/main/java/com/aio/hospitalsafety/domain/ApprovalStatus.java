// PGH
package com.aio.hospitalsafety.domain;

/**
 * TB_EMP.AUTH_ST에 저장되는 가입 승인 상태다.
 * PENDING 계정은 가입했지만 관리자의 승인을 받기 전 상태다.
 * APPROVED 계정만 관제 기능을 사용할 수 있도록 Security 규칙을 연결해야 한다.
 */
public enum ApprovalStatus {
    PENDING,  // 승인 대기: 신규 가입 기본값
    APPROVED, // 승인 완료
    INACTIVE  // 비활성화
}
