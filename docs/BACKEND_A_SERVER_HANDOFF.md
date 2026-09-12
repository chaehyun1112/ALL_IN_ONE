> **[CODEX 생성 파일]** Backend A 서버 구현과 팀 인계를 위해 추가한 문서입니다.

# Backend A 서버 구현 인계 문서

## 구현 범위

- 병원 관리자가 같은 병원의 일반 사용자 계정을 발급한다.
- 서버가 임시 비밀번호를 만들고 BCrypt 해시만 저장한다.
- 새 계정은 `ACTIVE`, `must_change_password = true`로 생성한다.
- 관리자는 같은 병원의 일반 사용자 계정만 비활성화하거나 재활성화한다.
- 계정 삭제 API는 제공하지 않는다.
- 병동 목록은 로그인한 관리자의 병원 범위로 제한한다.

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/admin/users` | 사용자 계정 발급 및 임시 비밀번호 1회 반환 |
| `PATCH` | `/api/admin/users/{userId}/disable` | 계정 비활성화 |
| `PATCH` | `/api/admin/users/{userId}/reactivate` | 계정 재활성화 |
| `GET` | `/api/admin/wards` | 현재 병원의 병동 선택지 조회 |

계정 발급 요청 예시:

```json
{
  "userId": "ward1.kim",
  "userName": "김간호",
  "wardId": 1,
  "role": "USER"
}
```

임시 비밀번호는 생성 응답에서만 평문으로 전달한다. 서버 로그와 DB에는 평문을 기록하지 않는다.

### 응답과 오류

- 계정 발급 성공: `201 Created`, 사용자 정보와 `temporaryPassword`, `mustChangePassword=true` 반환
- 상태 변경과 병동 조회 성공: `200 OK`
- 입력 누락, 다른 병원 병동, `ADMIN` 발급 시도: `400 Bad Request`
- 로그인하지 않은 요청: 로그인 화면으로 이동
- 일반 사용자의 관리자 API 요청: `403 Forbidden`
- 현재 병원에서 관리할 수 없는 사용자: `404 Not Found`
- 중복 아이디 또는 현재 상태와 맞지 않는 변경: `409 Conflict`
- `POST`, `PATCH` 요청에는 Spring Security의 요청 위조 방지 값(CSRF 토큰)이 필요하다.

관리 대상 사용자의 병원 ID는 브라우저가 보내지 않는다. 로그인한 관리자 정보의 병원 ID를 서버가 사용한다. 역할은 현재 구조에서 `USER`만 허용하므로, 병원에 지급된 관리자 계정으로 다른 관리자 계정을 생성할 수 없다.

## FRONTEND TODO

### 사용자 계정 생성

- 연결 API: `POST /api/admin/users`
- 입력: `userId`, `userName`, `wardId`, `role=USER`
- 병동 선택지: `GET /api/admin/wards`
- 성공 시 `temporaryPassword`를 관리자에게 한 번만 표시하고 복사할 수 있어야 한다.
- 창을 닫은 뒤 초기 비밀번호를 다시 조회하는 기능은 제공하지 않는다.

### 계정 상태 관리

- 비활성화: `PATCH /api/admin/users/{userId}/disable`
- 재활성화: `PATCH /api/admin/users/{userId}/reactivate`
- 상태값은 `ACTIVE`, `DISABLED`만 표시한다.

### 다른 담당 화면

- 최초 로그인 비밀번호 변경, 일반 비밀번호 변경, 관리자 비밀번호 재설정 결과 표시
- 로그인 화면의 “아이디 또는 비밀번호 분실 시 병원 관리자 문의” 안내
- 감사 로그 조회 화면은 Backend C의 조회 계약 확정 후 연결

## 팀 작업과 충돌 지점

| 담당 | 작업 | 주 수정 영역 | 충돌 주의 |
| --- | --- | --- | --- |
| Backend A | 계정 발급, 임시 비밀번호, 상태 변경 | 새 `admin` Controller/Service/Mapper/DTO | `SecurityConfig`에 관리자 URL 규칙 1건 추가 |
| Backend B | 로그인, 최초 변경 강제, 비밀번호 변경/재설정 | `User`, `UserMapper`, `UserService`, `HospitalUserDetails`, `SecurityConfig` | `SecurityConfig`는 A 변경을 보존해 병합 |
| Backend C | 기존 승인 흐름 제거, 감사 로그, 사용자 관리 조회 | 기존 관리자 기능과 새 감사 로그 계층 | A 서비스 성공 시점에 감사 로그 연결 필요 |
| Frontend D | 기존 화면 연결 및 새 화면 설계 | HTML/JS/CSS | CSRF와 1회성 임시 비밀번호 응답 처리 |

`SecurityConfig`는 여러 담당자가 만질 가능성이 가장 높다. Backend A의 `/api/admin/**` 권한 규칙을 유지한 상태에서 Backend B의 최초 비밀번호 변경 예외 경로를 추가하는 순서가 안전하다.

## 월요일 DB 회의 전 제한

현재 DB 구조는 이 API의 목표 구조와 다르다. 신규 API를 실제 DB에 호출하지 않고 컴파일과 기존 화면 기동까지만 검증한다.

1. `AUTH_ST` 체크 제약을 제거한 뒤 기존 `APPROVED`를 `ACTIVE`, 나머지 상태를 `DISABLED`로 변환한다.
2. `AUTH_ST` 허용값을 `ACTIVE`, `DISABLED`로 다시 제한한다.
3. `MUST_CHANGE_PASSWORD BOOLEAN NOT NULL` 컬럼과 기본값을 결정한다.
4. 기존 사용자에게 최초 변경을 요구할지 정하고 초기 데이터를 반영한다.
5. 현재 `EMP_EMAIL`은 `NOT NULL UNIQUE`이다. 관리자 발급 시 이메일을 받을지 nullable로 변경할지 결정한다.
6. 현재 `EMP_ID`는 모든 병원에서 하나만 쓸 수 있다. 이 방식을 유지할지, 병원마다 같은 직원 ID를 쓸 수 있게 `(HOSP_DIV_ID, EMP_ID)` 두 값을 묶을지 결정한다.
7. 발급·상태 변경·비밀번호 재설정 감사 로그 테이블의 컬럼과 보존 기간을 결정한다.

## 다른 담당자와 합칠 지점

- 로그인 담당은 `User`, `UserMapper`, `HospitalUserDetails`를 `AccountStatus`와 `mustChangePassword` 기준으로 전환한다.
- `DISABLED` 사용자의 로그인을 막고, 이미 로그인한 경우 언제 로그아웃시킬지도 정한다.
- 최초 로그인 비밀번호 강제 변경과 관리자 비밀번호 재설정 API를 연결한다.
- 작업 기록 담당은 계정 생성·상태 변경이 성공했을 때만 기록되게 연결한다. 한쪽만 저장되는 일이 없어야 한다.

## 작업 전에 꼭 확인

이 문서는 Backend A가 만든 계정 발급 기능과 Backend B·C가 이어서 할 일을 설명한다. Java 코드는 새 DB 구조에 맞춰져 있지만 현재 DB는 아직 예전 승인 구조다. DB 구조를 바꾸기 전에는 관리자 계정 생성·상태 변경 API를 실제 DB에 호출하면 안 된다.

권장 작업 순서는 다음과 같다.

1. 팀 전체가 `AUTH_ST`, `MUST_CHANGE_PASSWORD`, `EMP_EMAIL`, `EMP_ID` 키 정책을 먼저 합의한다.
2. Backend B가 로그인 모델과 비밀번호 흐름을 목표 스키마로 전환한다.
3. Backend C가 기존 승인 흐름을 제거하고 감사 로그·사용자 조회를 연결한다.
4. 세 사람의 코드를 합친 브랜치에서 DB 변경문을 적용한다.
5. 마지막으로 실제 테스트 DB에서 계정 생성, 비활성화, 재활성화가 처음부터 끝까지 되는지 확인한다.

DB만 먼저 바꾸면 기존 `UserMapper`가 `ACTIVE/DISABLED` 값을 읽지 못해 로그인이 깨질 수 있다. 반대로 Java 코드만 먼저 합치면 관리자 SQL이 현재 DB 컬럼과 맞지 않는다. 따라서 2~4번은 한 번에 맞춰야 한다.

## Backend A 생성 폴더와 파일 안내

### `controller/admin`

- `UserProvisioningController`: 관리자 요청을 받는 곳이다. 계정 생성, 비활성화, 재활성화, 병동 조회를 처리한다. 병원 ID는 요청 값이 아니라 로그인한 관리자의 `hospitalId`를 사용한다.
- `AdminApiExceptionHandler`: Backend A API에서 발생한 입력 오류, 사용자 없음, 중복·상태 충돌을 각각 400, 404, 409로 변환한다.

### `dto/admin`

- `CreateUserRequest`: 계정 발급 입력이다. 병원 ID는 의도적으로 포함하지 않는다.
- `CreateUserResponse`: 생성된 사용자 정보와 최초 한 번만 보여 줄 `temporaryPassword`를 반환한다.
- `UserStatusResponse`: 비활성화·재활성화 결과를 반환한다.
- `WardOptionResponse`: 현재 관리자 병원의 병동 선택지를 반환한다.

### `service/admin`

- `UserProvisioningService`: 역할, 병원, 병동, 중복 ID, 현재 상태를 확인한 뒤 DB 작업을 처리한다. 중간에 실패하면 앞에서 처리한 내용도 함께 취소된다.
- `TemporaryPasswordGenerator`: `SecureRandom`으로 12자리 임시 비밀번호를 만든다. 대문자·소문자·숫자·특수문자를 각각 하나 이상 포함한다.

### `mapper/admin` 및 XML

- `UserProvisioningMapper`: 서비스에서 사용할 DB 작업 메서드 목록이다.
- `UserProvisioningMapper.xml`: `TB_EMP`, `TB_WARD` 대상 SQL이다. 상태 변경 SQL은 반드시 `HOSP_DIV_ID`, `ROLE_CD='USER'`, 현재 상태 조건을 유지해야 한다.

### 공통 파일

- `AccountStatus`: 목표 계정 상태 `ACTIVE`, `DISABLED`만 정의한다.
- `UserConflictException`: 중복 ID 또는 잘못된 상태 전이다.
- `UserNotFoundException`: 로그인 관리자의 병원에서 관리할 수 없는 사용자다.
- `SecurityConfig`: `/api/admin/**`에 `ROLE_ADMIN` 제한 한 줄이 추가됐다.

## 꼭 지켜야 할 규칙

- 화면에서 보낸 병원 ID를 그대로 믿지 않는다. 항상 로그인한 관리자 정보인 `HospitalUserDetails#getHospitalId()`를 사용한다.
- 관리자는 `Role.USER`만 발급할 수 있다.
- 생성 전 선택 병동이 로그인 관리자의 병원 소속인지 검사한다.
- 비밀번호 평문은 Mapper, DB, 감사 로그로 전달하지 않는다.
- 임시 비밀번호는 생성 성공 응답에서만 한 번 반환하고 재조회 API를 만들지 않는다.
- 생성 응답의 `Cache-Control: no-store`를 유지한다.
- 상태 변경 SQL의 병원·USER·현재 상태 조건을 제거하지 않는다.
- 사용자 DELETE API나 물리 삭제 SQL을 추가하지 않는다.
- Spring Security의 요청 위조 방지 기능(CSRF)을 끄지 않는다.
- `SecurityConfig`의 `/api/admin/**` 규칙은 `.anyRequest().authenticated()`보다 앞에 있어야 한다.

## Backend B 인계 체크리스트

Backend B가 주로 수정할 파일은 다음과 같다.

- `domain/User.java`
- `mapper/UserMapper.java`, `resources/mapper/UserMapper.xml`
- `config/HospitalUserDetails.java`
- `config/SecurityConfig.java`
- `service/UserService.java`
- `controller/UserPasswordController.java`

작업 시 다음을 함께 반영한다.

1. `User`와 `UserMapper`가 `ApprovalStatus` 대신 새 상태값인 `AccountStatus`를 사용하게 바꾸고 `MUST_CHANGE_PASSWORD`도 조회한다.
2. `DISABLED` 사용자는 인증 단계에서 로그인할 수 없게 한다.
3. `mustChangePassword=true` 사용자는 허용된 비밀번호 변경·로그아웃 경로 외의 업무 화면에 접근하지 못하게 한다.
4. 최초 비밀번호 변경 성공 시 BCrypt 해시를 저장하고 `MUST_CHANGE_PASSWORD=false`로 갱신한다.
5. 관리자 비밀번호 재설정 성공 시 새 임시 비밀번호의 BCrypt 해시를 저장하고 `MUST_CHANGE_PASSWORD=true`로 되돌린다.
6. `HospitalUserDetails`의 `hospitalId`와 `ROLE_ADMIN` 생성 규칙은 유지한다.
7. `SecurityConfig`를 합칠 때 Backend A의 `/api/admin/**` 관리자 규칙과 요청 위조 방지 기능(CSRF)을 유지한다.
8. 병원 ID와 직원 ID를 묶어서 키로 사용하게 되면 기존 `updatePassword` SQL에도 `HOSP_DIV_ID` 조건을 추가한다.

Backend B 완료 조건:

- `DISABLED` 로그인 차단
- 임시 비밀번호 로그인 후 변경 강제
- 변경 완료 후 기존 로그인 상태를 끝내거나 다시 로그인하도록 처리
- 일반 비밀번호 변경과 관리자 재설정의 용어·API 분리
- 비밀번호 평문·임시 비밀번호 로그 없음

## Backend C 인계 체크리스트

Backend C는 기존 승인 기능 제거, 사용자 관리 조회, 감사 로그를 담당한다.

1. 승인 흐름을 제거할 때 Backend B의 `AccountStatus` 전환이 끝났는지 먼저 확인한다.
2. 사용자 목록·상세 조회는 로그인 관리자의 `HOSP_DIV_ID`와 `ROLE_CD='USER'` 조건을 사용한다.
3. 사용자 관리 조회 상태는 `ACTIVE/DISABLED`만 노출한다.
4. 계정 생성·비활성화·재활성화·비밀번호 재설정 기록은 실제 DB 변경과 함께 저장한다. 한쪽만 저장되는 일이 없어야 한다.
5. 감사 로그에는 임시 비밀번호 평문, 사용자 입력 비밀번호, BCrypt 해시를 저장하지 않는다.
6. 과거 안전 기록 보존을 위해 사용자 물리 삭제 기능을 추가하지 않는다.
7. Backend A 서비스에 작업 기록을 연결할 때 성공한 `insertUser`, `disableUser`, `reactivateUser` 뒤에만 기록한다. 오류가 나면 사용자 변경과 작업 기록을 모두 취소해야 한다.

감사 로그에 필요한 최소 정보 후보는 수행 병원, 수행 관리자 ID, 대상 사용자 ID, 작업 유형, 작업 시각이다. 실제 컬럼명과 보존 기간은 DB 회의에서 확정한다.

## 같이 수정할 가능성이 큰 파일

| 파일 | 주로 수정할 담당자 | 반드시 남겨야 할 내용 |
| --- | --- | --- |
| `SecurityConfig.java` | Backend B | Backend A의 `/api/admin/** hasRole("ADMIN")`, 요청 위조 방지 기능 |
| `User.java` | Backend B | `hospitalId`, `Role`, 목표 `AccountStatus`, `mustChangePassword` |
| `UserMapper.xml` | Backend B | 병원+사용자 조회 조건, 비밀번호 변경 시 병원 조건 검토 |
| `UserProvisioningService.java` | Backend A/C | A의 병원·역할 확인, C의 작업 기록을 한 작업으로 연결 |
| `UserProvisioningMapper.xml` | Backend A/DB | 병원·USER·현재 상태 조건과 새 DB 컬럼 |

한 파일을 두 담당자가 동시에 크게 고치지 말고, 위 표의 주 담당자가 먼저 완료한 뒤 다른 담당자가 작은 연결 변경을 얹는 방식을 권장한다.

## 테스트와 검증 상태

2026-09-12 기준 확인 결과:

- Java 21 `clean package`: 성공
- 기존 프로젝트 테스트: 11개 성공, 실패 0
- Backend A 관리자 API와 서비스 기능 확인: 9개 성공, 실패 0
- 실제 DB 대신 잠깐 만든 테스트 DB에서 SQL 확인: 3개 성공, 실패 0
- 로그인·대시보드·로그아웃 기능 확인: 3개 성공, 실패 0
- 전체 자동 테스트: 26개 성공, 실패 0
- 운영 DB INSERT/UPDATE: 실행하지 않음
- `/login`: 200
- 비로그인 `/api/admin/wards`: `/login`으로 302
- 로그인·비밀번호 재설정 화면: 데스크톱과 390×844 모바일에서 가로 넘침 및 브라우저 콘솔 오류 없음
- `login.css`, `password-reset.css`, `signup.css`, 현재 JS와 배경 이미지 경로: 200

테스트 주의사항: DB SQL 테스트를 새로 만들 때는 테스트 설정에 `mybatis.mapper-locations`를 적어야 한다. 현재 테스트용 `application.properties`가 기본 설정을 덮기 때문이다. 이 설정이 없으면 Mapper 객체는 만들어져도 실제 SQL을 찾지 못해 `Invalid bound statement` 오류가 난다.

## Backend A 외 화면·경로 발견사항

다음은 Backend A가 만든 파일의 결함은 아니지만 Frontend D 또는 기존 화면 정리 작업에서 처리해야 한다.

- `templates/html/signup.html`은 `/signup` Controller가 없어 현재 서비스 경로로 열리지 않는다.
- `signup.html`의 클래스명과 `signup.css`의 주요 선택자(`signup-body`, `signup-header`, `signup-form`, `signup-field`, `signup-submit`)가 일치하지 않아 경로를 다시 연결하더라도 대부분의 스타일이 적용되지 않는다.
- `signup.css`가 존재하지 않는 `/image/chevron-down.svg`를 참조한다.
- `static/JS/signup/ID_check.js`의 `/api/users/check-username`과 `static/JS/password-rest/ID_check.js`의 `/api/auth/check-id`는 구현된 Controller 경로가 아니며 현재 HTML에서도 사용되지 않는다.

관리자 발급 방식이 최종 정책이므로 기존 회원가입 화면과 관련 JS를 새 관리자 화면으로 재사용할지 제거할지는 Frontend D와 Backend C가 합의한다. 임의로 기존 승인·회원가입 경로를 되살리면 Backend A의 “관리자만 USER 발급” 정책과 충돌한다.

## 통합 완료 후 최종 점검

- [ ] DB 상태값과 Java `AccountStatus`가 일치한다.
- [ ] `MUST_CHANGE_PASSWORD`가 조회·생성·변경·재설정 흐름에 모두 연결됐다.
- [ ] 관리자 API가 로그인 관리자의 병원 범위를 벗어나지 않는다.
- [ ] `ROLE_ADMIN` 계정 발급이 400으로 거절된다.
- [ ] 다른 병원 병동 선택이 400으로 거절된다.
- [ ] 상태 전이 중복은 409, 대상 없음은 404다.
- [ ] POST/PATCH에 요청 위조 방지 값(CSRF 토큰)이 없으면 403이다.
- [ ] 임시 비밀번호는 응답 한 번 외에 어디에도 남지 않는다.
- [ ] 감사 로그에 비밀번호 관련 값이 없다.
- [ ] 사용자 삭제 API와 DELETE SQL이 없다.
- [ ] 실제 테스트 DB에서 계정 생성·비활성화·재활성화가 처음부터 끝까지 정상 동작한다.
