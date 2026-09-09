// PGH
package com.aio.hospitalsafety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 애플리케이션의 시작점이다.
 *
 * @SpringBootApplication은 다음 기능을 한 번에 활성화한다.
 * - 현재 패키지 아래의 @Controller, @Service, @Mapper 등을 탐색
 * - 의존성에 맞는 Spring Boot 자동 설정 적용
 * - Java 기반 설정 클래스 사용
 */
@SpringBootApplication
public class HospitalSafetyApplication {

	public static void main(String[] args) {
		// 내장 Tomcat 서버와 Spring ApplicationContext를 실행한다.
		SpringApplication.run(HospitalSafetyApplication.class, args);
	}

}
