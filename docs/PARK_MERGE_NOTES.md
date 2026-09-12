# park 26428c2 병합 기록

## 유지한 구현

- Backend A의 계정 발급 Controller/Service/Mapper/XML, 임시 비밀번호 생성기,
  DTO, 예외 처리 및 `AccountStatus`는 원본을 유지했다.
- `/api/admin/**` 관리자 제한과 CSRF를 유지했다.
- 현재 jg의 도메인 선택, 로그인 오류 구분, 비활성 계정 로그인 차단,
  기존 세션 만료, 로그아웃, 대시보드와 조치기록 화면을 유지했다.
- 기존 `HomeController`와 `DashboardController`가 로그인·대시보드 경로를
  담당하므로 park의 구형 `AuthController`는 중복 등록하지 않았다.
- 중복된 `GET /api/admin/wards`는 Backend A의 `UserProvisioningController`가
  담당한다. 응답 필드 `wardId`, `wardName`은 기존 화면과 같다.
- park의 세션 쿠키 설정과 30분 만료 설정, Maven wrapper 수정을 반영했다.
- 삭제했던 아이디 찾기·사용자 비밀번호 재설정 화면은 복원하지 않았다.

## 화면 파일 구조

현재 jg의 HTML, CSS, JS `auth` 폴더 구조를 유지한다.

| 구분 | 위치 |
| --- | --- |
| 도메인·로그인·비밀번호 변경·회원가입·조치기록 | `src/main/resources/templates/html/auth/` |
| 위 화면의 CSS | `src/main/resources/static/css/auth/` |
| 로그인·비밀번호 변경·회원가입·조치기록·세션 확인 JS | `src/main/resources/static/JS/auth/` |

컨트롤러 템플릿 이름, HTML 상대 경로, Thymeleaf 정적 리소스 경로,
CSS 배경 이미지 경로 및 park에서 가져온 테스트의 화면 참조를 이 구조에 맞췄다.
직접 열기에서도 스타일과 스크립트가 로드되도록 도메인·회원가입 화면 경로를 수정했다.
관리자 화면은 `templates/html/admin/`에 유지한다.
JS 하위 폴더는 `admin`, `auth`만 남기고 기존 스크립트는 `auth`로 옮겼다.
`JS/dashboard.js`는 기존 위치를 유지하고 비어 있던 `find-id` 폴더를 삭제했다.

## DB 및 후속 통합 제한

- 이 병합에서는 DB 변경문이나 실제 계정 생성·상태 변경 요청을 실행하지 않는다.
- jg의 로그인 모델은 현재 `APPROVED/INACTIVE`를 사용한다.
- Backend A의 새 API는 목표 스키마인 `ACTIVE/DISABLED`와
  `MUST_CHANGE_PASSWORD`를 그대로 전제로 한다. 따라서 새 계정 생성·상태 변경
  API를 현재 DB에서 호출하면 안 된다.
- `AUTH_ST`, `MUST_CHANGE_PASSWORD`, 이메일 필수 여부, 직원 ID 키 정책을
  먼저 합의하고 Backend B/C 구현과 DB 변경을 함께 맞춰야 한다.
- 기존 승인 흐름·공개 회원가입·사용자 삭제 정책은 Backend C 및 화면 담당자와
  후속 정리가 필요하다. 이번 화면 경로 병합에서 정책을 임의 변경하지 않았다.
- 자세한 담당 범위와 작업 순서는 원본 `BACKEND_A_SERVER_HANDOFF.md`를 따른다.
- 테스트 폴더는 실행하지 않고 Maven의 `-Dmaven.test.skip=true`를 유지한다.
