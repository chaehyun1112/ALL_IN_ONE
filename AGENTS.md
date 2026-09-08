# 프로젝트 네이밍 규칙

이 규칙은 프로젝트 전체와 앞으로 추가하거나 수정하는 백엔드 코드에 적용한다.

- 일반 사용자 도메인은 `User`, 관리자 도메인은 `Admin`으로 통일한다.
- 사용자 명칭으로 `Member`를 사용하지 않는다. `member`, `MEMBER` 및 이를 포함하는 클래스명, 변수명, 메서드명, 키도 사용하지 않는다.
- 일반 사용자 계정이나 접속 유형을 직무명인 `Nurse` / `nurse`로 명명하지 않는다. 화면에 표시하는 직무명(예: 간호사)은 유지할 수 있다.
- 클래스는 PascalCase, 변수와 메서드는 camelCase, 상수와 enum 값은 UPPER_SNAKE_CASE를 사용한다.

| 적용 대상 | 일반 사용자 | 관리자 |
| --- | --- | --- |
| Entity | `User` | `Admin` |
| DTO | `UserDto`, `UserLoginRequest`, `UserLoginResponse` | `AdminDto`, `AdminLoginRequest`, `AdminLoginResponse` |
| Repository | `UserRepository` | `AdminRepository` |
| MyBatis Mapper | `UserMapper` | `AdminMapper` |
| Service | `UserService` | `AdminService` |
| Controller | `UserController` | `AdminController` |
| 변수 및 필드 | `user`, `userId`, `userRepository`, `loginUser` | `admin`, `adminId`, `adminRepository`, `loginAdmin` |
| 메서드 | `findUserById`, `loginUser` | `findAdminById`, `loginAdmin` |
| 세션 키 | `loginUser` | `loginAdmin` |
| 세션 키 상수 | `LOGIN_USER` | `LOGIN_ADMIN` |
| 접속 유형 문자열 | `user` | `admin` |
| 역할 상수 및 enum 값 | `USER` | `ADMIN` |

- 로그인 세션을 구현할 때 세션 키는 공통 상수로 정의하고 저장, 조회, 삭제에서 동일한 상수를 사용한다.
- 패키지명, 파일명, API 필드 및 프런트엔드 내부 식별자에도 같은 도메인 용어를 적용한다.
- 명칭 변경 시 선언뿐 아니라 참조, 템플릿, 매핑, 테스트도 함께 수정하여 동작이 일치하도록 한다.

# 개발 서버 실행 규칙

- 서버에 반영이 필요한 변경 후에는 사용자가 직접 재시작하도록 안내하는 대신 이 프로젝트의 개발 서버를 자동으로 재시작하고 HTTP 응답을 확인한다.
- 재시작할 때는 이 프로젝트의 서버 프로세스인지 확인하고, 다른 프로젝트의 프로세스는 종료하지 않는다.
