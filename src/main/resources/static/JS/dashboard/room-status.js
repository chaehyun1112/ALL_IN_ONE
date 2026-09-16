"use strict";

/* [09.13]추가내용: 병실 상태 데이터와 상태명을 대시보드 공통 설정으로 분리합니다. */
window.CareGuardRoomStatus = {
  rooms: new Map(Array.from({length:17}, (_,i) => [301+i, {
    number:301+i, status:i===4?"urgent":i===11?"caution":"normal", acknowledged:false
  }])),
  labels: {normal:"정상", caution:"침대 이탈", urgent:"낙상 감지"}
};

/* 현재 예시 경보와 수신 이벤트의 이력입니다. 서버 연결 시 당일 이력을 적재합니다. */
(() => {
  const state = window.CareGuardRoomStatus;
  const events = new Map();
  const dateFormat = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit"
  });
  state.dayKey = value => dateFormat.format(new Date(value));
  state.addEvent = ({id, room, type, occurredAt = Date.now()}) => {
    const time = new Date(occurredAt).getTime();
    if (id == null || !String(id).trim() || !state.rooms.has(Number(room)) ||
        !["urgent", "caution"].includes(type) || !Number.isFinite(time)) return false;
    const key = String(id);
    if (events.has(key)) return false;
    events.set(key, {room: Number(room), type, occurredAt: time});
    return true;
  };
  state.todayCounts = (number, now = Date.now()) => {
    const counts = {urgent: 0, caution: 0};
    const today = state.dayKey(now);
    for (const event of events.values()) {
      if (event.room === Number(number) && state.dayKey(event.occurredAt) === today && event.occurredAt <= now) {
        counts[event.type]++;
      }
    }
    return counts;
  };
  // [2026.09.16] 추가한 내용: 선택 병실의 오늘 기록 중 실제 발생 시각이 가장 최근인 이벤트를 조회합니다.
  state.latestTodayEvent = (number, now = Date.now()) => {
    let latest = null;
    const today = state.dayKey(now);
    for (const event of events.values()) {
      if (event.room === Number(number) && state.dayKey(event.occurredAt) === today &&
          event.occurredAt <= now && (!latest || event.occurredAt > latest.occurredAt)) latest = event;
    }
    return latest ? {...latest} : null;
  };
  // 기존 화면 예시 경보만 오늘의 예시 이력으로 초기화합니다. 새로고침하면 예시 상태로 돌아갑니다.
  for (const room of state.rooms.values()) {
    if (room.status === "normal") continue;
    room.eventId = `demo-${room.number}-${state.dayKey(Date.now())}`;
    state.addEvent({id: room.eventId, room: room.number, type: room.status});
  }
})();
