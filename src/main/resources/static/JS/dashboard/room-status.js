"use strict";

/* [09.13]추가내용: 병실 상태 데이터와 상태명을 대시보드 공통 설정으로 분리합니다. */
window.CareGuardRoomStatus = {
  rooms: new Map(Array.from({length:17}, (_,i) => [301+i, {
    number:301+i, status:i===4?"urgent":i===11?"caution":"normal", acknowledged:false
  }])),
  labels: {normal:"안전 정상", caution:"침대 이탈", urgent:"낙상 감지"}
};
