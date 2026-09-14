# 객체 및 변수 네이밍 규칙

이 프로젝트는 Spring MVC와 Layered Architecture를 사용하며, 모든 레이어에서 아래 명칭을 동일하게 적용한다.

## 1. 일반 사용자

- 일반 사용자를 나타내는 영문 명칭은 `User`로 통일한다.
- 예: `User`, `UserDto`, `UserMapper`, `UserService`, `UserController`
- 변수와 메서드는 `user`, `userId`, `userName`, `userList`, `findUser`처럼 작성한다.
- URL과 화면 파일은 `/user/**`, `user-*.html` 형식을 사용한다.
- 사용자 정보를 세션에 직접 추가할 경우 키 이름은 `LOGIN_USER_ID`처럼 `USER`를 사용한다.

## 2. 관리자

- 관리자를 나타내는 영문 명칭은 `Admin`으로 통일한다.
- 예: `AdminDto`, `AdminService`, `AdminController`
- 변수와 메서드는 `admin`, `adminId`, `findAdmin`처럼 작성한다.
- 현재 DB의 `ROLE_CD = ADMIN`은 사용자의 역할을 나타내는 값이므로 그대로 사용한다.

## 3. 금지 명칭

- 객체, DTO, Mapper/Repository, Service, Controller, 변수, 메서드, 세션 키에 `Member`를 사용하지 않는다.
- 일반 사용자를 `Account`, `Staff`, `Employee`로 새로 이름 짓지 않는다. 계정이라는 일반 개념을 설명하는 한국어 문장에는 사용할 수 있다.

## 4. 현재 DB 물리명과 Java 이름의 차이

PostgreSQL에는 이미 `TB_EMP`, `EMP_ID`, `EMP_PW`, `EMP_NM`이 존재한다. 이 이름들은 테이블 명세서에 정해진 물리명이므로 변경하지 않는다.

MyBatis Mapper에서 다음처럼 Java 이름으로 변환하여 사용한다.

- `TB_EMP` 한 행 → `User`
- `EMP_ID` → `userId`
- `EMP_PW` → `passwordHash`
- `EMP_NM` → `userName`

즉, SQL에서는 실제 DB 컬럼명을 사용하고 Java 코드에서는 `User` 규칙을 사용한다.
