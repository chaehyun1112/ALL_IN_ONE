package com.aio.hospitalsafety.dto.sms;

/**
 * 낙상 SMS 를 받을 사람 한 명.
 *
 * userType 은 TB_SMS_SEND_HISTORY.USER_TYPE 값과 같다.
 * EMP: 병동 간호사, CAREGIVER: 병실 담당 간병인
 * userId 는 둘 다 tb_emp.emp_id 다(간병인도 지금은 tb_emp 에 있다. 명세의 CAREGIVER_ID 와 다르다).
 */
public record SmsRecipient(
        String userType,
        String userId,
        String phoneNumber
) {
}
