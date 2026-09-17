"use strict";

/* [09.13]추가내용: 조치 기록의 병동·상태·대상·검색어 조건 계산을 전용 모듈로 분리합니다. */
window.CareGuardRecordFilter = {
  matchesConditions(record, conditions) {
    const names = conditions.target === "patient"
      ? record.patient
      : conditions.target === "staff"
        ? record.staff
        : `${record.patient} ${record.staff}`;
    return (
      (conditions.room === "all" || record.room === conditions.room) &&
      (conditions.status === "all" || record.status === conditions.status) &&
      names.toLocaleLowerCase().includes(conditions.keyword)
    );
  }
};
