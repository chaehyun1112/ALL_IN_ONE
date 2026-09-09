// PGH
package com.aio.hospitalsafety.domain;

/**
 * 실제 PostgreSQL의 TB_HOSPITAL 한 행을 표현한다.
 * HOSP_DIV_ID는 병원 구분 ID이자 PK이고, HOSP_NM은 화면에 표시할 병원명이다.
 */
public record Hospital(String hospitalId, String hospitalName) {
}
