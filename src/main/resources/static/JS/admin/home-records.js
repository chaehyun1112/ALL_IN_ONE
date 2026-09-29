"use strict";

/* [2026.09.27 추가] 관리자 홈의 예시 카드 4개를 서버 데이터로 채웁니다.
   데이터: /api/action-history (병원 전체 확정 낙상, 최신순, 조치 여부 포함), /api/admin/wards (병동 목록)
   - 최근 조치기록: 최신 2건
   - 실시간 알림: 최근 24시간 안에 발생했고 아직 조치가 없는(미확인) 낙상, 최신 3건 (자정 직전 낙상이 0시에 사라지지 않게)
   - 병동별·시간대별 사고 발생 현황: 오늘 발생한 낙상 건수
   관리자 홈이 보이는 동안 15초마다 다시 불러옵니다.
   Live Server 미리보기(서버 주소 없음)에서는 HTML 의 예시를 그대로 둡니다. */
(() => {
  const home = document.querySelector("#admin-home-page");
  const recordsUrl = home?.dataset.recordsUrl;
  const wardsUrl = home?.dataset.wardsUrl;
  if (!recordsUrl || !wardsUrl) return;

  const REFRESH_MS = 15_000;
  // 시간대 구간은 화면 디자인(팀원 담당)의 세 구간 그대로 둡니다. 00~06시 낙상은 이 차트에 들어가지 않습니다(2026.09.28 되돌림).
  const TIME_SLOTS = [
    { label: "오전", range: "06–12시", from: 6, to: 12 },
    { label: "오후", range: "12–18시", from: 12, to: 18 },
    { label: "저녁", range: "18–24시", from: 18, to: 24 }
  ];

  // 한국 날짜 "2026-09-27"
  function todayKey() {
    return new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit"
    }).format(new Date());
  }

  async function getJson(url) {
    const response = await fetch(url, {
      credentials: "same-origin",
      cache: "no-store",
      headers: { Accept: "application/json" }
    });
    if (!response.ok || response.redirected) throw new Error(`${url} ${response.status}`);
    return response.json();
  }

  // 서버 값은 textContent 로만 넣습니다(HTML 로 해석하지 않게).
  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  // "2026-09-26T20:59" → "2026.09.26 20:59"
  function screenDateTime(value) {
    return `${value.slice(0, 10).replaceAll("-", ".")} ${value.slice(11, 16)}`;
  }

  function hideDemoBadge(section) {
    section.querySelectorAll(".admin-home-alert-demo").forEach(badge => { badge.hidden = true; });
  }

  function renderRecentRecords(records) {
    const section = home.querySelector(".admin-home-recent-records");
    const list = section.querySelector(".admin-home-record-list");
    list.replaceChildren();
    if (!records.length) list.append(element("div", "admin-home-record-row", "아직 낙상 기록이 없습니다."));
    for (const record of records.slice(0, 2)) {
      const row = element("div", "admin-home-record-row");
      const text = element("div");
      text.append(element("strong", "", `${record.room} · ${record.type}`), element("small", "", screenDateTime(record.occurredAt)));
      row.append(text, element("span", record.status === "완료" ? "is-complete" : "is-pending", record.status));
      list.append(row);
    }
    hideDemoBadge(section);
  }

  // 서버 기록과 같은 모양의 한국 시각 "2026-09-26T20:59"
  function seoulMinute(date) {
    const parts = Object.fromEntries(new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit", hourCycle: "h23"
    }).formatToParts(date).map(part => [part.type, part.value]));
    return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`;
  }

  function renderAlerts(records) {
    const section = home.querySelector(".admin-home-alerts");
    const list = section.querySelector(".admin-home-alert-list");
    // [2026.09.29 변경] 실시간 알림 카드는 제목과 분리한 숨김 스크롤 목록 안에서만 교체합니다.
    list.replaceChildren();
    const since = seoulMinute(new Date(Date.now() - 24 * 60 * 60 * 1000));
    const pending = records.filter(record => record.status !== "완료" && record.occurredAt >= since).slice(0, 3);
    if (!pending.length) list.append(element("div", "admin-home-record-row", "지금 확인이 필요한 낙상 알림이 없습니다."));
    for (const record of pending) {
      const row = element("div", "admin-home-alert-row");
      const indicator = element("span", "admin-home-alert-indicator");
      indicator.setAttribute("aria-hidden", "true");
      const details = element("div", "admin-home-alert-details");
      details.append(element("strong", "", `${record.wardName} ${record.room}`.trim()), element("span", "", "낙상 감지 · 미확인"));
      const time = element("time", "", record.occurredAt.slice(11, 16));
      time.dateTime = record.occurredAt;
      row.append(indicator, details, time);
      list.append(row);
    }
    hideDemoBadge(section);
  }

  function renderWardChart(todayFalls, wards) {
    const section = home.querySelector(".admin-home-ward-summary");
    const chart = section.querySelector(".admin-home-ward-chart");
    const counts = wards.map(ward => ({
      name: ward.wardName,
      count: todayFalls.filter(record => record.wardName === ward.wardName).length
    }));
    const max = Math.max(1, ...counts.map(item => item.count));
    chart.replaceChildren();
    for (const { name, count } of counts) {
      const column = element("div", "admin-home-ward-column");
      const track = element("div", "admin-home-ward-track");
      const bar = element("span", "is-fall");
      bar.style.height = `${Math.round(count / max * 100)}%`;
      bar.title = `낙상 ${count}건`;
      track.append(bar);
      column.append(element("strong", "", `${count}건`), track, element("span", "", name), element("small", "", `낙상 ${count}건`));
      chart.append(column);
    }
    chart.setAttribute("aria-label", `오늘 집계: ${counts.map(item => `${item.name} 낙상 ${item.count}건`).join(", ")}`);
    hideDemoBadge(section);
  }

  function renderTimeChart(todayFalls) {
    const section = home.querySelector(".admin-home-time-summary");
    const chart = section.querySelector(".admin-home-time-chart");
    const counts = TIME_SLOTS.map(slot => ({
      ...slot,
      count: todayFalls.filter(record => {
        const hour = Number(record.occurredAt.slice(11, 13));
        return hour >= slot.from && hour < slot.to;
      }).length
    }));
    const max = Math.max(1, ...counts.map(item => item.count));
    chart.replaceChildren();
    for (const slot of counts) {
      const row = element("div", "admin-home-time-row");
      const label = element("span");
      label.append(element("strong", "", slot.label), element("small", "", slot.range));
      const track = element("div", "admin-home-time-track");
      const bar = element("i", "is-fall");
      bar.style.width = `${Math.round(slot.count / max * 100)}%`;
      bar.title = `낙상 ${slot.count}건`;
      track.append(bar);
      row.append(label, track, element("b", "", `${slot.count}건`));
      chart.append(row);
    }
    chart.setAttribute("aria-label", `오늘 집계: ${counts.map(item => `${item.label} 낙상 ${item.count}건`).join(", ")}`);
    hideDemoBadge(section);
  }

  let wards = null;
  let timer = null;

  async function refresh() {
    clearTimeout(timer);
    try {
      if (!wards) wards = await getJson(wardsUrl);
      const records = await getJson(recordsUrl);
      const today = todayKey();
      const todayFalls = records.filter(record => record.occurredAt.startsWith(today));
      renderRecentRecords(records);
      renderAlerts(records);
      renderWardChart(todayFalls, wards);
      renderTimeChart(todayFalls);
    } catch (error) {
      console.warn("관리자 홈 기록을 불러오지 못했습니다.", error);
    } finally {
      timer = setTimeout(refreshWhenVisible, REFRESH_MS);
    }
  }

  // 다른 관리자 메뉴를 보고 있거나 창이 숨겨져 있으면 서버를 부르지 않고 기다립니다.
  function refreshWhenVisible() {
    if (document.visibilityState === "visible" && !home.hidden) refresh();
    else timer = setTimeout(refreshWhenVisible, REFRESH_MS);
  }

  refresh();
})();
