package com.aio.hospitalsafety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.aio.hospitalsafety")
@MapperScan("com.aio.hospitalsafety.mapper")
// [2026.10.01] 영상 보관 기간이 지난 파일 정리(EventMediaService.removeExpiredClips)
@EnableScheduling
public class HospitalSafetyApplication {

	public static void main(String[] args) {
		SpringApplication.run(HospitalSafetyApplication.class, args);
	}

}
