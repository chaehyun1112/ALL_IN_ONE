"use strict";

// [2026.09.29 변경] 관리자 사고 기록은 확정 낙상 발생 이력을 별도 화면에서 조회합니다.
// 조치기록 API를 사용하되, 사고의 발생 시각·위치·처리 상태를 중심으로 다시 표시합니다.
(() => {
  const page = document.querySelector("#admin-accident-view");
  if (!page) return;

  const recordsUrl = page.dataset.recordsUrl;
  const rows = document.querySelector("#admin-accident-rows");
  const result = document.querySelector("#admin-accident-result");
  const form = document.querySelector("#admin-accident-filter-form");
  const statusFilter = document.querySelector("#admin-accident-status");
  const wardFilter = document.querySelector("#admin-accident-ward");
  const searchInput = document.querySelector("#admin-accident-search");
  const resetButton = document.querySelector("#admin-accident-reset");
  const pagination = document.querySelector("#admin-accident-pagination");
  const total = document.querySelector("#admin-accident-total");
  const pending = document.querySelector("#admin-accident-pending");
  const completed = document.querySelector("#admin-accident-completed");
  const recordsPerPage = 18;
  let allRecords = [];
  let filteredRecords = [];
  let currentPage = 1;

  const demoRecords = [
    { occurredAt: "2026-09-29T10:12", wardName: "3병동", room: "302호", patient: "김OO", type: "낙상 감지", status: "미확인", completedAt: "" },
    { occurredAt: "2026-09-29T08:44", wardName: "2병동", room: "205호", patient: "이OO", type: "낙상 감지", status: "완료", completedAt: "2026-09-29T08:51" },
    { occurredAt: "2026-09-28T19:26", wardName: "1병동", room: "107호", patient: "박OO", type: "낙상 감지", status: "완료", completedAt: "2026-09-28T19:33" }
  ];

  function displayDateTime(value) {
    if (!value) return "-";
    return `${value.slice(0, 10).replaceAll("-", ".")} ${value.slice(11, 16)}`;
  }

  function text(value) {
    return value?.trim() || "-";
  }

  function addCell(row, value, className = "") {
    const cell = document.createElement("td");
    if (className) cell.className = className;
    cell.textContent = value;
    row.append(cell);
  }

  function updateSummary() {
    total.textContent = allRecords.length;
    pending.textContent = allRecords.filter(record => record.status !== "완료").length;
    completed.textContent = allRecords.filter(record => record.status === "완료").length;
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
        renderRows();
      });
      pagination.append(button);
    }
  }

  function renderRows() {
    rows.replaceChildren();
    const totalPages = Math.max(1, Math.ceil(filteredRecords.length / recordsPerPage));
    currentPage = Math.min(currentPage, totalPages);
    const start = (currentPage - 1) * recordsPerPage;
    const visibleRecords = filteredRecords.slice(start, start + recordsPerPage);

    if (!visibleRecords.length) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 7;
      cell.className = "admin-accident-empty";
      cell.textContent = "검색 조건에 맞는 사고 기록이 없습니다.";
      row.append(cell);
      rows.append(row);
    } else {
      visibleRecords.forEach(record => {
        const row = document.createElement("tr");
        addCell(row, displayDateTime(record.occurredAt));
        addCell(row, text(record.wardName));
        addCell(row, text(record.room));
        addCell(row, text(record.patient));
        addCell(row, text(record.type || "낙상 감지"), "admin-accident-type");
        addCell(row, record.status === "완료" ? "완료" : "미확인", record.status === "완료" ? "is-completed" : "is-pending");
        addCell(row, displayDateTime(record.completedAt));
        rows.append(row);
      });
    }
    result.textContent = `사고 기록 ${filteredRecords.length}건`;
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
      const sameStatus = statusFilter.value === "all" || record.status === statusFilter.value;
      const sameWard = wardFilter.value === "all" || record.wardName === wardFilter.value;
      const searchable = [record.room, record.patient, record.wardName, record.type]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return sameStatus && sameWard && (!query || searchable.includes(query));
    });
    currentPage = 1;
    renderRows();
  }

  async function reload() {
    result.textContent = "사고 기록을 불러오는 중입니다.";
    try {
      const response = await fetch(recordsUrl, {
        credentials: "same-origin",
        cache: "no-store",
        headers: { Accept: "application/json" }
      });
      if (!response.ok || response.redirected) throw new Error(`사고 기록 조회 실패: ${response.status}`);
      allRecords = await response.json();
    } catch (error) {
      // [2026.09.29 변경] Live Server 미리보기에서는 서버 API 대신 화면 확인용 예시를 표시합니다.
      if (location.protocol === "file:" || location.port === "5500") {
        allRecords = demoRecords;
      } else {
        allRecords = [];
        result.textContent = "사고 기록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
      }
    }
    updateWardOptions();
    applyFilters();
  }

  form.addEventListener("submit", event => {
    event.preventDefault();
    applyFilters();
  });
  statusFilter.addEventListener("change", applyFilters);
  wardFilter.addEventListener("change", applyFilters);
  searchInput.addEventListener("input", applyFilters);
  resetButton.addEventListener("click", () => {
    statusFilter.value = "all";
    wardFilter.value = "all";
    searchInput.value = "";
    applyFilters();
  });

  // [2026.09.29 변경] 상단 메뉴의 사고 기록을 다시 선택해도 최신 사고 목록을 갱신합니다.
  window.AdminAccidentRecords = { reload };
  if (!page.hidden) reload();
})();
