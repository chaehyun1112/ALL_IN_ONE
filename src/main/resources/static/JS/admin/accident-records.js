"use strict";

// [2026.09.29 변경] 관리자 사고 기록을 영상 보관함으로 전환합니다.
// 감지 이벤트의 event_detail.video_url(또는 videoUrl)을 받아 목록 선택 시 video 요소로 재생합니다.
(() => {
  const page = document.querySelector("#admin-accident-view");
  if (!page) return;

  const recordsUrl = page.dataset.recordsUrl;
  const list = document.querySelector("#admin-accident-rows");
  const form = document.querySelector("#admin-accident-filter-form");
  const wardFilter = document.querySelector("#admin-accident-ward");
  const searchInput = document.querySelector("#admin-accident-search");
  const resetButton = document.querySelector("#admin-accident-reset");
  const pagination = document.querySelector("#admin-accident-pagination");
  const pendingVideoCount = document.querySelector("#admin-accident-pending-video-count");
  const completedVideoCount = document.querySelector("#admin-accident-completed-video-count");
  const videoStatusCards = Array.from(document.querySelectorAll("[data-video-status]"));
  const video = document.querySelector("#admin-accident-video");
  const videoEmpty = document.querySelector("#admin-accident-video-empty");
  const recordsPerPage = 12;
  let allRecords = [];
  let filteredRecords = [];
  let currentPage = 1;
  let selectedEventId = "";
  let selectedVideoStatus = "all";

  const demoRecords = [
    { eventId: "demo-video-1", occurredAt: "2026-09-29T10:12", wardName: "3병동", room: "302호", patient: "김OO", type: "낙상 감지", status: "미확인", completedAt: "", videoUrl: "" },
    { eventId: "demo-video-2", occurredAt: "2026-09-29T08:44", wardName: "2병동", room: "205호", patient: "이OO", type: "낙상 감지", status: "완료", completedAt: "2026-09-29T08:51", videoUrl: "" },
    { eventId: "demo-video-3", occurredAt: "2026-09-28T19:26", wardName: "1병동", room: "107호", patient: "박OO", type: "낙상 감지", status: "완료", completedAt: "2026-09-28T19:33", videoUrl: "" }
  ];

  function displayDateTime(value) {
    if (!value) return "-";
    return `${value.slice(0, 10).replaceAll("-", ".")} ${value.slice(11, 16)}`;
  }

  function text(value) {
    return value?.trim() || "-";
  }

  function hasVideo(record) {
    return Boolean(record.videoUrl?.trim());
  }

  function updateSummary() {
    // [2026.09.29 변경] 영상 보관함의 상태 카드는 저장 주소 유무와 관계없이 미확인·확인 완료 사고 건수를 각각 표시합니다.
    pendingVideoCount.textContent = allRecords.filter(record => record.status === "미확인").length;
    completedVideoCount.textContent = allRecords.filter(record => record.status === "완료").length;
  }

  function renderVideoStatusCards() {
    videoStatusCards.forEach(card => {
      const selected = card.dataset.videoStatus === selectedVideoStatus;
      card.classList.toggle("is-selected", selected);
      card.setAttribute("aria-pressed", String(selected));
    });
  }

  function clearPlayer() {
    selectedEventId = "";
    video.pause();
    video.removeAttribute("src");
    video.load();
    video.hidden = true;
    videoEmpty.hidden = false;
    videoEmpty.querySelector("strong").textContent = "사고 영상을 선택해 주세요.";
    videoEmpty.querySelector("span").textContent = "선택한 영상이 이 영역에서 재생됩니다.";
  }

  function showVideo(record) {
    selectedEventId = record.eventId;
    if (!hasVideo(record)) {
      video.pause();
      video.removeAttribute("src");
      video.load();
      video.hidden = true;
      videoEmpty.hidden = false;
      videoEmpty.querySelector("strong").textContent = "재생할 영상이 없습니다.";
      videoEmpty.querySelector("span").textContent = "저장된 영상이 등록되면 이 영역에서 재생됩니다.";
      renderCards();
      return;
    }
    video.src = record.videoUrl;
    video.hidden = false;
    videoEmpty.hidden = true;
    video.load();
    renderCards();
  }

  function createCard(record) {
    const card = document.createElement("button");
    card.type = "button";
    card.className = "admin-accident-video-card";
    card.classList.toggle("is-selected", record.eventId === selectedEventId);
    card.classList.toggle("is-missing", !hasVideo(record));
    card.setAttribute("aria-pressed", String(record.eventId === selectedEventId));
    card.setAttribute("aria-label", `${displayDateTime(record.occurredAt)} ${text(record.room)} 사고 영상 선택`);

    const metadata = document.createElement("span");
    metadata.className = "admin-accident-video-meta";
    const timestamp = document.createElement("time");
    timestamp.dateTime = record.occurredAt || "";
    timestamp.textContent = displayDateTime(record.occurredAt);
    const state = document.createElement("strong");
    state.className = record.status === "완료" ? "is-completed" : "is-pending";
    state.textContent = record.status === "완료" ? "완료" : "미확인";
    metadata.append(timestamp, state);

    const title = document.createElement("strong");
    title.className = "admin-accident-video-title";
    title.textContent = `${[record.wardName, record.room].filter(Boolean).join(" · ") || "위치 미정"} · ${text(record.type || "낙상 감지")}`;
    const subtitle = document.createElement("span");
    subtitle.className = "admin-accident-video-subtitle";
    subtitle.textContent = text(record.patient);
    card.append(metadata, title, subtitle);
    card.addEventListener("click", () => showVideo(record));
    return card;
  }

  function renderPagination() {
    pagination.replaceChildren();
    const totalPages = Math.ceil(filteredRecords.length / recordsPerPage);
    if (totalPages <= 1) return;
    for (let pageNumber = 1; pageNumber <= totalPages; pageNumber += 1) {
      const button = document.createElement("button");
      button.type = "button";
      button.textContent = pageNumber;
      button.setAttribute("aria-label", `${pageNumber}페이지`);
      if (pageNumber === currentPage) button.setAttribute("aria-current", "page");
      button.addEventListener("click", () => {
        currentPage = pageNumber;
        renderCards();
      });
      pagination.append(button);
    }
  }

  function renderCards() {
    list.replaceChildren();
    const totalPages = Math.max(1, Math.ceil(filteredRecords.length / recordsPerPage));
    currentPage = Math.min(currentPage, totalPages);
    const start = (currentPage - 1) * recordsPerPage;
    const pageRecords = filteredRecords.slice(start, start + recordsPerPage);
    if (!pageRecords.length) {
      const empty = document.createElement("p");
      empty.className = "admin-accident-empty";
      empty.textContent = "검색 조건에 맞는 사고 영상이 없습니다.";
      list.append(empty);
    } else {
      pageRecords.forEach(record => list.append(createCard(record)));
    }
    renderPagination();
  }

  function updateWardOptions() {
    const selectedValue = wardFilter.value;
    const wards = [...new Set(allRecords.map(record => record.wardName).filter(Boolean))];
    wardFilter.replaceChildren(new Option("전체 병동", "all"));
    wards.forEach(ward => wardFilter.add(new Option(ward, ward)));
    wardFilter.value = wards.includes(selectedValue) ? selectedValue : "all";
  }

  function applyFilters() {
    const query = searchInput.value.trim().toLowerCase();
    filteredRecords = allRecords.filter(record => {
      const sameStatus = selectedVideoStatus === "all" || record.status === selectedVideoStatus;
      const sameWard = wardFilter.value === "all" || record.wardName === wardFilter.value;
      const searchable = [record.room, record.patient, record.wardName, record.type]
        .filter(Boolean).join(" ").toLowerCase();
      return sameStatus && sameWard && (!query || searchable.includes(query));
    });
    currentPage = 1;
    clearPlayer();
    renderCards();
  }

  async function reload() {
    try {
      const response = await fetch(recordsUrl, {
        credentials: "same-origin",
        cache: "no-store",
        headers: { Accept: "application/json" }
      });
      if (!response.ok || response.redirected) throw new Error(`사고 영상 조회 실패: ${response.status}`);
      allRecords = await response.json();
    } catch (error) {
      // [2026.09.29 변경] Live Server 미리보기에서는 저장소 연결 없이 보관함 UI를 확인할 수 있습니다.
      if (location.protocol === "file:" || location.port === "5500") allRecords = demoRecords;
      else allRecords = [];
    }
    updateSummary();
    updateWardOptions();
    renderVideoStatusCards();
    applyFilters();
  }

  form.addEventListener("submit", event => { event.preventDefault(); applyFilters(); });
  wardFilter.addEventListener("change", applyFilters);
  searchInput.addEventListener("input", applyFilters);
  resetButton.addEventListener("click", () => {
    selectedVideoStatus = "all";
    renderVideoStatusCards();
    wardFilter.value = "all";
    searchInput.value = "";
    applyFilters();
  });

  videoStatusCards.forEach(card => card.addEventListener("click", event => {
    event.stopPropagation();
    selectedVideoStatus = selectedVideoStatus === card.dataset.videoStatus ? "all" : card.dataset.videoStatus;
    renderVideoStatusCards();
    applyFilters();
  }));
  // [2026.09.29 변경] 카드 밖의 빈 화면을 누르면 상태별 목록을 닫고 전체 목록으로 되돌립니다.
  document.addEventListener("click", event => {
    if (selectedVideoStatus === "all" || event.target.closest(".admin-accident-overview, .admin-accident-list-column, .admin-accident-player, .admin-accident-filters")) return;
    selectedVideoStatus = "all";
    renderVideoStatusCards();
    applyFilters();
  });

  // [2026.09.29 변경] 상단 메뉴의 사고 기록을 다시 선택하면 저장된 영상 목록을 최신 상태로 갱신합니다.
  window.AdminAccidentRecords = { reload };
  if (!page.hidden) reload();
})();
