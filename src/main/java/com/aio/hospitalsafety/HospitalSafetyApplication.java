// PGH
package com.aio.hospitalsafety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * Spring Boot 애플리케이션의 시작점이다.
 *
 * @SpringBootApplication은 다음 기능을 한 번에 활성화한다.
 * - 현재 패키지 아래의 @Controller, @Service, @Mapper 등을 탐색
 * - 의존성에 맞는 Spring Boot 자동 설정 적용
 * - Java 기반 설정 클래스 사용
 *
 * 메인 클래스는 항상 최상위 패키지(com.aio.hospitalsafety)에 둔다.
 * 그래야 별도의 scanBasePackages 지정 없이 controller/service/mapper/config 등
 * 하위 패키지가 전부 자동으로 스캔된다.
 */
@SpringBootApplication
@MapperScan("com.aio.hospitalsafety.mapper")
public class HospitalSafetyApplication {

	public static void main(String[] args) {
		// 내장 Tomcat 서버와 Spring ApplicationContext를 실행한다.
		SpringApplication.run(HospitalSafetyApplication.class, args);
	}

}
