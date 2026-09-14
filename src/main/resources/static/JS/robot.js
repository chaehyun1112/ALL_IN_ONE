"use strict";
document.addEventListener("DOMContentLoaded", () => {
  /* [참고] 병실 상태는 대시보드 화면과 같은 예시 데이터입니다.
     실제 연동 시에는 서버에서 같은 병실 상태를 함께 받아와 이 화면에도 반영하세요. */
  const rooms = new Map(Array.from({ length: 17 }, (_, i) => [301 + i, {
    number: 301 + i, status: "normal"
  }]));
  const labels = { normal: "안전 정상", caution: "침대 이탈", urgent: "낙상 감지" };

  const upper = document.getElementById("upper-rooms");
  const lower = document.getElementById("lower-rooms");
  const nodes = new Map();

  function addRoom(number, parent) {
    const node = document.createElement("div");
    node.className = "room";
    node.innerHTML = `<strong>${number}호</strong><small></small><span class="door" aria-hidden="true"></span>`;
    nodes.set(number, node);
    parent.append(node);
  }
  function facility() {
    const el = document.createElement("div");
    el.className = "facility";
    el.innerHTML = '<span class="symbol" aria-hidden="true">WC</span><span>화장실</span>';
    return el;
  }
  for (let n = 301; n <= 310; n++) addRoom(n, upper);
  lower.append(facility());
  for (let n = 311; n <= 313; n++) addRoom(n, lower);
  const gap = document.createElement("div");
  gap.setAttribute("aria-hidden", "true");
  lower.append(gap);
  for (let n = 314; n <= 317; n++) addRoom(n, lower);
  lower.append(facility());

  /* 병실 상태를 화면에 반영합니다(읽기 전용). */
  for (const room of rooms.values()) {
    const node = nodes.get(room.number);
    node.classList.toggle("urgent", room.status === "urgent");
    node.classList.toggle("caution", room.status === "caution");
    node.querySelector("small").textContent = labels[room.status];
  }

  /* ---------------------------------------------------------
     순찰 로봇: 평상시엔 정해진 경로를 순찰하고,
     낙상 감지 같은 긴급 병실이 생기면 그 병실로 즉시 이동합니다.
     실제 로봇 위치 연동 전까지는 화면 시연용 시뮬레이션입니다.
     --------------------------------------------------------- */
  const ROBOT_PATH = [301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 317, 316, 315, 314, 313, 312, 311];
  const LEG_MS = 1800;
  const floorEl = document.getElementById("robot-floor");
  const corridorEl = document.querySelector("#robot-floor .corridor");
  const robotMarker = document.getElementById("robot-marker");
  const robotStatusEl = document.getElementById("robot-status");
  const robotStatusText = document.getElementById("robot-status-text");
  const modeCard = document.getElementById("robot-mode-card");
  const modeLabel = document.getElementById("robot-mode-label");
  const locationLabel = document.getElementById("robot-location-label");
  const targetLabel = document.getElementById("robot-target-label");
  const logList = document.getElementById("robot-log-list");
  const logTotal = document.getElementById("robot-log-total");

  let robotPatrolIndex = 0;
  let robotMode = "patrol";
  let robotDispatchRoom = null;
  let robotArriveTimer = null;
  let robotResolveTimer = null;
  let currentX = 0, currentY = 0, travelId = 0;
  const logs = [];

  /* .floor 기준 상대 좌표(중심점)를 구합니다. */
  function relativeCenter(el) {
    const floorRect = floorEl.getBoundingClientRect();
    const rect = el.getBoundingClientRect();
    return {
      x: rect.left - floorRect.left + rect.width / 2,
      y: rect.top - floorRect.top + rect.height / 2
    };
  }
  function roomCenter(number) {
    const node = nodes.get(number);
    return node ? relativeCenter(node) : null;
  }
  function corridorY() {
    return corridorEl ? relativeCenter(corridorEl).y : null;
  }

  /* 로봇은 병실 안으로 들어가지 않고 항상 중앙 복도 위에서만 좌우로 움직입니다.
     특정 병실의 x좌표까지만 이동하고, y는 항상 복도 높이로 고정합니다. */
  function corridorPoint(number) {
    const room = roomCenter(number);
    const cy = corridorY();
    if (!room || cy == null) return null;
    return { x: room.x, y: cy };
  }

  /* 로봇 마커를 중간 애니메이션 없이 즉시 특정 병실 앞 복도 위치로 놓습니다(초기 배치·화면 크기 변경용). */
  function positionRobotAt(number) {
    if (!floorEl || !robotMarker) return;
    const point = corridorPoint(number);
    if (!point) return;
    robotMarker.style.left = `${point.x}px`;
    robotMarker.style.top = `${point.y}px`;
    currentX = point.x;
    currentY = point.y;
    if (locationLabel) locationLabel.textContent = `${number}호 인근`;
  }

  function wait(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  /* 복도를 따라서만 좌우로 이동합니다(병실 안으로는 들어가지 않습니다). */
  async function travelViaCorridor(number) {
    const point = corridorPoint(number);
    if (!point || !robotMarker) {
      positionRobotAt(number);
      return;
    }
    const myTravelId = ++travelId;
    const moveTo = (x, y) => {
      robotMarker.style.left = `${x}px`;
      robotMarker.style.top = `${y}px`;
      currentX = x;
      currentY = y;
    };

    moveTo(point.x, point.y);
    await wait(LEG_MS);
    if (myTravelId !== travelId) return;

    if (locationLabel) locationLabel.textContent = `${number}호 인근`;
  }

  function addLog(text, kind) {
    const time = new Intl.DateTimeFormat("ko-KR", {
      timeZone: "Asia/Seoul", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false
    }).format(new Date());
    logs.unshift({ time, text, kind });
    renderLog();
  }

  function renderLog() {
    if (!logList) return;
    logList.replaceChildren();
    if (!logs.length) {
      const empty = document.createElement("p");
      empty.className = "robot-log-empty";
      empty.textContent = "아직 로봇 대응 이력이 없습니다.";
      logList.append(empty);
    }
    for (const entry of logs) {
      const card = document.createElement("article");
      card.className = "alert-card robot-log" + (entry.kind === "arrived" ? " log-arrived caution" : "");
      card.innerHTML = `<div class="alert-top"><strong>● ${entry.kind === "arrived" ? "도착" : "출동"}</strong><time>${entry.time}</time></div><p>${entry.text}</p>`;
      logList.append(card);
    }
    if (logTotal) logTotal.textContent = `${logs.length}건`;
  }

  function setRobotStatus(mode, statusText, modeText) {
    robotMode = mode;
    robotMarker?.classList.toggle("dispatch", mode === "dispatch");
    robotMarker?.classList.toggle("arrived", mode === "arrived");
    robotStatusEl?.classList.toggle("dispatch", mode === "dispatch");
    robotStatusEl?.classList.toggle("arrived", mode === "arrived");
    modeCard?.classList.toggle("dispatch", mode === "dispatch");
    modeCard?.classList.toggle("arrived", mode === "arrived");
    modeCard?.classList.toggle("patrol", mode === "patrol");
    if (robotStatusText) robotStatusText.textContent = statusText;
    if (modeLabel) modeLabel.textContent = modeText;
  }

  function patrolStep() {
    if (robotMode !== "patrol") return;
    robotPatrolIndex = (robotPatrolIndex + 1) % ROBOT_PATH.length;
    const number = ROBOT_PATH[robotPatrolIndex];
    travelViaCorridor(number);
    setRobotStatus("patrol", `병동 순찰 중입니다 · ${number}호 인근`, "순찰 중");
    if (targetLabel) targetLabel.textContent = "-";
  }

  function dispatchRobotTo(number) {
    if (robotDispatchRoom === number) return;
    robotDispatchRoom = number;
    clearTimeout(robotArriveTimer);
    clearTimeout(robotResolveTimer);
    travelViaCorridor(number);
    setRobotStatus("dispatch", `🚨 ${number}호로 긴급 이동 중입니다`, "출동 중");
    if (targetLabel) targetLabel.textContent = `${number}호`;
    addLog(`${number}호 낙상 감지 신호 수신 → 로봇 출동 시작`, "dispatch");
    robotArriveTimer = setTimeout(() => {
      setRobotStatus("arrived", `✅ ${number}호에 도착해 확인 중입니다`, "도착");
      addLog(`${number}호 도착 · 현장 확인 중`, "arrived");
      /* [무한루프] 도착 후 잠시 확인하는 시늉을 한 뒤, 병실을 정상으로 되돌리고
         다시 순찰로 복귀시킵니다. 실제 연동 시엔 "대응 완료" 처리 시점에 맞춰 호출하세요. */
      robotResolveTimer = setTimeout(() => {
        const room = rooms.get(number);
        if (room) room.status = "normal";
        const node = nodes.get(number);
        if (node) {
          node.classList.remove("urgent", "caution");
          node.querySelector("small").textContent = labels.normal;
        }
        addLog(`${number}호 확인 완료 · 정상으로 전환`, "arrived");
        checkDispatch();
      }, 4000);
    }, 2200);
  }

  function resumeRobotPatrol() {
    robotDispatchRoom = null;
    clearTimeout(robotArriveTimer);
    clearTimeout(robotResolveTimer);
    setRobotStatus("patrol", `병동 순찰 중입니다 · ${ROBOT_PATH[robotPatrolIndex]}호 인근`, "순찰 중");
    if (targetLabel) targetLabel.textContent = "-";
  }

  /* 긴급 병실이 있으면 즉시 그쪽으로 출동시키고, 없어지면 순찰로 복귀합니다. */
  function checkDispatch() {
    const urgentRoom = [...rooms.values()].find((room) => room.status === "urgent");
    if (urgentRoom) {
      if (robotDispatchRoom !== urgentRoom.number) dispatchRobotTo(urgentRoom.number);
    } else if (robotDispatchRoom !== null) {
      resumeRobotPatrol();
    }
  }

  positionRobotAt(ROBOT_PATH[0]);
  setRobotStatus("patrol", `병동 순찰 중입니다 · ${ROBOT_PATH[0]}호 인근`, "순찰 중");
  renderLog();
  checkDispatch();
  /* [수정] 다음 구간 이동을 이전 이동이 끝나는 시점(LEG_MS)에 맞춰 바로 이어서
     시작합니다. 간격을 이동 시간보다 길게 두면 중간에 멈췄다 가는 것처럼 보입니다. */
  setInterval(patrolStep, LEG_MS);

  window.addEventListener("resize", () => {
    positionRobotAt(robotMode === "patrol" ? ROBOT_PATH[robotPatrolIndex] : robotDispatchRoom);
  });

  /* [참고] 실제 서버 연동 시 새 낙상 이벤트가 오면 아래처럼 호출해 로봇을 출동시키세요.
     window.RobotView.notifyFallEvent(305); */
  window.RobotView = {
    notifyFallEvent(number) {
      if (!rooms.has(Number(number))) return false;
      rooms.get(Number(number)).status = "urgent";
      const node = nodes.get(Number(number));
      node.classList.add("urgent");
      node.querySelector("small").textContent = labels.urgent;
      checkDispatch();
      return true;
    }
  };

  /* 공통 헤더 메뉴·시계·로그아웃(대시보드 화면과 동일한 동작) */
  const toggle = document.getElementById("menu-toggle");
  const menu = document.getElementById("header-menu-list");
  function closeMenu() { menu.hidden = true; toggle.setAttribute("aria-expanded", "false"); }
  toggle?.addEventListener("click", () => {
    menu.hidden = !menu.hidden;
    toggle.setAttribute("aria-expanded", String(!menu.hidden));
  });
  document.addEventListener("click", (e) => { if (!e.target.closest(".header-menu")) closeMenu(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape") closeMenu(); });
  document.getElementById("logout-open")?.addEventListener("click", () => { window.location.href = "/logout"; });
  const updateClock = () => {
    const clock = document.getElementById("clock");
    if (clock) {
      clock.textContent = new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false
      }).format(new Date());
    }
  };
  updateClock();
  setInterval(updateClock, 1000);
});
