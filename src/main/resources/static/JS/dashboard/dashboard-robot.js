"use strict";
/* [추가] 메인 대시보드 평면도 위에 순찰 로봇 마커를 띄운다.
   병실 카드/경보 로직은 dashboard.js가 그대로 담당하고, 이 파일은 로봇 위치
   애니메이션만 담당한다(같은 window.CareGuardRoomStatus.rooms 상태를 읽기만 함,
   render()나 dashboard.js의 내부 상태는 건드리지 않는다). /robot 화면의
   robot.js와 같은 순찰·출동 로직을 재사용한다. */
document.addEventListener("DOMContentLoaded", () => {
  const rooms = window.CareGuardRoomStatus?.rooms;
  const floorEl = document.querySelector(".floor-panel .floor");
  const corridorEl = floorEl?.querySelector(".corridor");
  if (!rooms || !floorEl || !corridorEl) return;

  const marker = document.createElement("div");
  marker.className = "robot-marker";
  marker.id = "dashboard-robot-marker";
  marker.setAttribute("aria-hidden", "true");
  marker.textContent = "🤖";
  floorEl.append(marker);

  function findRoomNode(number) {
    const rows = floorEl.querySelectorAll(".room-row .room");
    for (const el of rows) {
      if (el.querySelector("strong")?.textContent === `${number}호`) return el;
    }
    return null;
  }

  const ROBOT_PATH = [301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 317, 316, 315, 314, 313, 312, 311];
  const ROBOT_SPEED = 0.03; // px/ms, robot.js와 동일한 속도
  const FALLBACK_LEG_MS = 1800;

  let robotPatrolIndex = 0;
  let robotMode = "patrol";
  let robotDispatchRoom = null;
  let robotResolveTimer = null;
  let currentX = 0, currentY = 0, travelId = 0;

  function relativeCenter(el) {
    const floorRect = floorEl.getBoundingClientRect();
    const rect = el.getBoundingClientRect();
    return { x: rect.left - floorRect.left + rect.width / 2, y: rect.top - floorRect.top + rect.height / 2 };
  }
  function corridorPoint(number) {
    const node = findRoomNode(number);
    if (!node) return null;
    return { x: relativeCenter(node).x, y: relativeCenter(corridorEl).y };
  }
  function positionRobotAt(number) {
    const point = corridorPoint(number);
    if (!point) return;
    marker.style.left = `${point.x}px`;
    marker.style.top = `${point.y}px`;
    currentX = point.x;
    currentY = point.y;
  }
  function wait(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }

  async function travelViaCorridor(number) {
    const point = corridorPoint(number);
    if (!point) { positionRobotAt(number); return FALLBACK_LEG_MS; }
    const myTravelId = ++travelId;
    const distance = Math.hypot(point.x - currentX, point.y - currentY);
    const duration = distance > 0 ? distance / ROBOT_SPEED : FALLBACK_LEG_MS;
    marker.style.transitionDuration = `${duration}ms`;
    marker.style.left = `${point.x}px`;
    marker.style.top = `${point.y}px`;
    currentX = point.x;
    currentY = point.y;
    await wait(duration);
    return myTravelId === travelId ? duration : duration;
  }

  function setRobotMode(mode) {
    robotMode = mode;
    marker.classList.toggle("dispatch", mode === "dispatch");
    marker.classList.toggle("arrived", mode === "arrived");
  }

  function patrolStep() {
    if (robotMode !== "patrol") return;
    robotPatrolIndex = (robotPatrolIndex + 1) % ROBOT_PATH.length;
    travelViaCorridor(ROBOT_PATH[robotPatrolIndex]).then(() => {
      if (robotMode === "patrol") patrolStep();
    });
  }

  async function dispatchRobotTo(number) {
    if (robotDispatchRoom === number) return;
    robotDispatchRoom = number;
    clearTimeout(robotResolveTimer);
    setRobotMode("dispatch");
    await travelViaCorridor(number);
    if (robotDispatchRoom !== number) return;
    setRobotMode("arrived");
    /* [참고] 실제 대응 완료 처리와는 별개로, 화면 시연용으로 잠시 후 순찰로 복귀한다.
       병실 상태 자체는 dashboard.js의 대응 등록 흐름이 바꾸므로 여기서는 건드리지 않는다. */
    robotResolveTimer = setTimeout(checkDispatch, 4000);
  }

  function resumeRobotPatrol() {
    robotDispatchRoom = null;
    clearTimeout(robotResolveTimer);
    setRobotMode("patrol");
    patrolStep();
  }

  function checkDispatch() {
    const urgentRoom = [...rooms.values()].find((room) => room.status === "urgent");
    if (urgentRoom) {
      if (robotDispatchRoom !== urgentRoom.number) dispatchRobotTo(urgentRoom.number);
    } else if (robotDispatchRoom !== null) {
      resumeRobotPatrol();
    }
  }

  positionRobotAt(ROBOT_PATH[0]);
  checkDispatch();
  if (robotMode === "patrol") patrolStep();

  /* dashboard.js가 새 낙상 이벤트를 받으면(window.CareGuard.receiveFallEvent) rooms 상태가
     바뀌므로, 주기적으로 확인해서 즉시 출동하게 한다(별도 이벤트 훅 없이 가볍게 폴링). */
  setInterval(checkDispatch, 1000);

  window.addEventListener("resize", () => {
    positionRobotAt(robotMode === "patrol" ? ROBOT_PATH[robotPatrolIndex] : robotDispatchRoom);
  });
});
