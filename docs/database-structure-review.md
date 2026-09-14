# 데이터베이스 구조 검토

검토 기준: `4-1. 테이블명세서 (1).pdf`, 데이터베이스 요구사항 분석서,
2026-09-08 AIO PostgreSQL의 `information_schema` 및 `pg_catalog` 읽기 전용 조회 결과.

## 확정되어 코드에 반영한 내용

- 직원 계정 테이블은 `TB_EMP`이다.
- 실제 DB에는 병원 마스터 `TB_HOSPITAL(HOSP_DIV_ID, HOSP_NM)`이 추가되어 있다.
- `EMP_ID VARCHAR(20)`가 별도 숫자 키 없이 기본키이며 로그인 ID로 사용된다.
- `EMP_PW VARCHAR(100)`에는 BCrypt 단방향 해시만 저장한다.
- 계정은 `HOSP_DIV_ID`, `WARD_ID`를 가진다.
- 역할은 `ADMIN`, `USER`, 승인 상태는 `PENDING`, `APPROVED`이다.
- 로그인 시 `ROLE_...`, `STATUS_...` 두 종류의 Spring Security 권한으로 변환한다.
- PW 변경 시 `EMP_PW`와 `UPD_DT`만 갱신하고 PW 확인값은 저장하지 않는다.
- 로그인은 `TB_HOSPITAL`의 병원 ID 확인 후 `HOSP_DIV_ID + EMP_ID + PW`를 검증하는 2단계 흐름이다.

## 목표 테이블 관계

```text
TB_HOSPITAL
  ├─< TB_WARD
  └─< TB_EMP

TB_WARD
  └─< TB_DEVICE
        ├─< TB_SAFETY_EVT
        └─< TB_DOOR ─< TB_DOOR_EVT
                  └──── TB_DOOR_EVT.DEV_ID도 TB_DEVICE 참조
```

현재 실제 DB에서 확인된 테이블은 `TB_HOSPITAL`, `TB_WARD`, `TB_EMP` 3개다.
장치와 이벤트 관련 테이블은 아직 실제 DB에 생성되지 않았다.

## TODO - 추가 확인 후 결정할 항목

### 1. 병원과 병동의 관계

PDF와 달리 실제 DB의 `TB_WARD.HOSP_DIV_ID`에는 단독 UNIQUE가 없고
`TB_HOSPITAL.HOSP_DIV_ID`를 참조하는 FK가 있다. 따라서 한 병원에 여러 병동 등록이 가능하다.

### 2. 직원의 병원 ID와 병동 일치 보장

실제 DB에서 `TB_EMP.HOSP_DIV_ID`는 `TB_HOSPITAL` FK이고 `TB_EMP.WARD_ID`는
`TB_WARD` FK다. 하지만 두 FK가 각각 독립적이어서 서로 다른 병원의 병원 ID와 병동 ID 조합도
저장될 수 있다. 복합 FK로 DB에서 보장할지, 가입/수정 Service에서 검증할지 결정해야 한다.

실제 `TB_EMP.WARD_ID`는 nullable이지만 요구사항과 PDF에는 필수값으로 작성되어 있다.
PENDING 단계에서는 병동 없이 가입하고 관리자가 승인 시 병동을 지정하는 것인지 확인해야 한다.

### 3. 병원 전용 로그인·회원가입 화면의 병원 식별 방식

사용자가 병원 구분 ID를 먼저 입력하고, 등록 병원 확인 후 직원 ID/PW를 입력하는
Dooray 방식의 2단계 로그인으로 확정되어 코드에 반영했다.

### 4. 비밀번호 분실 시 본인 확인 방식

6개 테이블에는 이메일, 휴대전화, 재설정 토큰, 임시 PW 관련 컬럼이 없다. 직원 ID와 직원명만으로 PW를 변경하면 타인이 쉽게 악용할 수 있다. 관리자 초기화, 사내 인증 또는 별도 인증 수단 중 하나를 확정해야 한다. 현재 `/password/reset`은 관리자 문의 안내만 제공한다.

### 5. 승인 계정의 관제 URL

관제 화면의 실제 URL이 아직 없다. URL 확정 후 `SecurityConfig`에서 해당 경로에 `STATUS_APPROVED` 권한 검사를 추가해야 한다. 병동별 데이터 제한은 URL 권한만으로 끝내지 말고 Service/Mapper 조회 조건에 로그인 사용자의 `HOSP_DIV_ID`, `WARD_ID`를 반드시 포함해야 한다.

### 6. 출입문 선택 상태 저장

`TB_DOOR`에는 좌표만 있고 이탈 감지 적용 여부 컬럼이 없다. `TB_DOOR`에 존재하는 행 자체가 선택된 출입문을 뜻하는지, 활성 여부를 별도로 저장할지 확인해야 한다.

### 7. 출입문 이벤트의 기본키

`TB_DOOR_EVT`에는 PK가 없다. 같은 문과 장치에서 같은 시각에 들어온 이벤트를 구분하거나 수정/삭제할 방법을 결정해야 한다. 이벤트 ID 추가 또는 복합키 사용 여부는 아직 확정하지 않는다.

### 8. 출입문과 장치 ID의 중복 참조 일관성

`TB_DOOR_EVT`는 `DOOR_ID`와 `DEV_ID`를 모두 저장하지만, 전달된 `DEV_ID`가 해당 문의 실제 장치인지 DB 제약조건이 확인하지 않는다. 중복 저장을 유지한다면 DB 또는 Service 계층에서 일치 검증이 필요하다.

### 9. 이벤트 유형 코드

`TB_SAFETY_EVT.EVT_TYPE_CD`의 정확한 저장 문자열과 CHECK 제약조건이 없다. 낙상, 침대 이탈, 액체, 고체 위험물, 통행 장애물의 영문 코드 목록을 먼저 확정해야 한다.

### 10. 처리 상태와 완료 정보의 일관성

`PROC_ST='COMPLETED'`일 때 `ACT_CONT`, `HANDLER_NM`, `COMPLETE_DT`가 필수인지 정해야 한다. 현재 명세만으로는 완료 상태인데 완료 일시가 없는 행도 저장할 수 있다.

### 11. 조회용 인덱스

요구사항의 직원명/직원 ID 검색과 사고 기록의 위치·유형·기간·상태·담당자 검색을 위한 인덱스가 명세에 없다. 실제 조회 SQL이 정해진 뒤 실행계획을 보고 인덱스를 결정한다.

### 12. 퇴사자 계정 삭제 방식

물리 삭제와 비활성화(soft delete) 중 어느 방식인지 명세에 없다. 감사 기록과 과거 조치 담당자 표시 정책을 확인한 뒤 결정한다.
