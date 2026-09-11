"use strict";

/* ==================================================
   기본 설정
   ================================================== */

/*
 * true: 화면 확인용 예시 데이터 사용
 * false: 서버 API 사용
 */
const settings = {
  useDemoData: true,
  demoToday: "2026-08-25"
};

/*
 * 전체 병실 목록
 * 실제 병원 병실 구성에 맞게 수정하세요.
 */
const roomNumbers = [
  "301호", "302호", "303호", "304호",
  "305호", "306호", "307호", "308호",
  "309호", "310호", "311호", "312호",
  "313호", "314호"
];

/*
 * 화면 확인용 예시 기록
 * 백엔드 연결 후에는 API 응답을 사용합니다.
 */
const demoRecords = [
  ["2026-08-25T14:41", "306호", "김OO", "낙상 감지", "", "미확인", ""],
  ["2026-08-25T14:39", "303호", "이OO", "침대 이탈", "박OO", "완료", "2026-08-25T14:43"],
  ["2026-08-25T14:37", "307호", "박OO", "낙상 감지", "정OO", "완료", "2026-08-25T14:42"],
  ["2026-08-25T14:28", "305호", "최OO", "침대 이탈", "고OO", "완료", "2026-08-25T14:33"],
  ["2026-08-25T14:10", "302호", "정OO", "낙상 감지", "임OO", "완료", "2026-08-25T14:17"],
  ["2026-08-24T21:06", "311호", "한OO", "침대 이탈", "강OO", "완료", "2026-08-24T21:11"],
  ["2026-08-24T18:32", "304호", "윤OO", "낙상 감지", "이OO", "완료", "2026-08-24T18:38"],
  ["2026-08-24T16:05", "309호", "서OO", "침대 이탈", "최OO", "완료", "2026-08-24T16:11"],
  ["2026-08-24T11:47", "301호", "오OO", "낙상 감지", "김OO", "완료", "2026-08-24T11:53"],
  ["2026-08-23T22:18", "314호", "문OO", "침대 이탈", "정OO", "완료", "2026-08-23T22:24"],
  ["2026-08-23T19:26", "308호", "배OO", "낙상 감지", "박OO", "완료", "2026-08-23T19:31"],
  ["2026-08-23T15:03", "312호", "송OO", "침대 이탈", "강OO", "완료", "2026-08-23T15:09"]
].map(([
  occurredAt,
  room,
  patient,
  type,
  staff,
  status,
  completedAt
]) => ({
  occurredAt,
  room,
  patient,
  type,
  staff,
  status,
  completedAt
}));

/* HTML을 모두 읽은 뒤 실행합니다. */
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializePage, {
    once: true
  });
} else {
  initializePage();
}

/* ==================================================
   화면 초기화
   ================================================== */

function initializePage() {
  /* 기본 화면 요소 */
  const historyPage = document.querySelector("#history-page");
  const searchForm = document.querySelector("#search-form");
  const recordList = document.querySelector("#record-list");
  const resultMessage = document.querySelector("#result-message");
  const tableScroll = document.querySelector(".table-scroll");
  const periodFilter = document.querySelector("#period-filter");

  /* 기간 달력 */
  const calendarDialog = document.querySelector("#calendar-dialog");
  const calendarMonth = document.querySelector("#calendar-month");
  const calendarDays = document.querySelector("#calendar-days");
  const calendarHelp = document.querySelector("#calendar-help");
  const previousMonthButton = document.querySelector("#previous-month");
  const nextMonthButton = document.querySelector("#next-month");
  const cancelCalendarButton = document.querySelector("#calendar-cancel");
  const confirmCalendarButton = document.querySelector("#calendar-confirm");

  /* 내보내기 팝업 */
  const exportButton = document.querySelector("#export-button");
  const exportDialog = document.querySelector("#export-dialog");
  const exportForm = document.querySelector("#export-form");
  const exportCount = document.querySelector("#export-count");
  const exportNote = document.querySelector("#export-note");
  const exportError = document.querySelector("#export-error");
  const exportSubmit = document.querySelector("#export-submit");

  /* 로그아웃 */
  const logoutButton = document.querySelector("#logout-button");

  /* 전체 기록과 현재 검색 결과 */
  let allRecords = [];
  let filteredRecords = [];

  /* 조회 상태 */
  let isLoading = false;
  let loadFailed = false;

  /* 직접 설정 기간 */
  let selectedStart = "";
  let selectedEnd = "";
  let draftStart = "";
  let draftEnd = "";
  let displayedMonth;

  /* 드롭다운 컨트롤 */
  const dropdownControls = [];
  let periodControl;

  /* ==================================================
     날짜와 시각
     ================================================== */

  /* 한국의 오늘 날짜를 YYYY-MM-DD로 반환합니다. */
  function getKoreanToday() {
    const parts = Object.fromEntries(
      new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul",
        year: "numeric",
        month: "2-digit",
        day: "2-digit"
      })
        .formatToParts(new Date())
        .map(part => [part.type, part.value])
    );

    return `${parts.year}-${parts.month}-${parts.day}`;
  }

  /* 예시 또는 실제 데이터 기준 날짜 */
  function getReferenceDay() {
    return settings.useDemoData
      ? settings.demoToday
      : getKoreanToday();
  }

  /* 최근 N일의 시작 날짜 */
  function getPeriodStart(endDay, days) {
    const start = new Date(`${endDay}T00:00:00Z`);

    start.setUTCDate(
      start.getUTCDate() - (days - 1)
    );

    return start.toISOString().slice(0, 10);
  }

  /* 달력 날짜를 YYYY-MM-DD로 변환합니다. */
  function dateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
  }

  /* 상단 현재 시각 */
  function updateClock() {
    const now = new Date();

    const parts = Object.fromEntries(
      new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul",
        year: "numeric",
        month: "numeric",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hourCycle: "h23"
      })
        .formatToParts(now)
        .map(part => [part.type, part.value])
    );

    const clock = document.querySelector("#current-time");

    clock.dateTime = now.toISOString();
    clock.textContent =
      `${parts.year}년 ${parts.month}월 ${parts.day}일 ` +
      `${parts.hour}:${parts.minute}`;
  }

  /* 표에 표시할 시간 */
  function formatTime(value) {
    if (!value) {
      return "-";
    }

    return (
      `${value.slice(5, 7)}.${value.slice(8, 10)} ` +
      value.slice(11, 16)
    );
  }

  /* ==================================================
     병실 목록
     ================================================== */

  /* 병실 목록을 버튼으로 만듭니다. */
  function renderRoomOptions() {
    const roomOptions = document.querySelector("#room-options");

    roomOptions.replaceChildren();

    const rooms = [
      {
        value: "all",
        label: "전체 병실"
      },
      ...roomNumbers.map(room => ({
        value: room,
        label: room
      }))
    ];

    rooms.forEach(room => {
      const button = document.createElement("button");

      button.type = "button";
      button.dataset.value = room.value;
      button.textContent = room.label;

      roomOptions.append(button);
    });
  }

  /* ==================================================
     드롭다운
     ================================================== */

  /*
   * 기간·병실·처리 상태·검색 대상에
   * 공통 드롭다운 기능을 연결합니다.
   */
  function createDropdown({
    root,
    trigger,
    menu,
    input,
    label,
    optionAttribute,
    onSelect
  }) {
    function getOptions() {
      return Array.from(
        menu.querySelectorAll(`button[${optionAttribute}]`)
      );
    }

    function getValue(option) {
      return option.getAttribute(optionAttribute);
    }

    function updateLabel() {
      const selected = getOptions().find(
        option => getValue(option) === input.value
      );

      if (selected) {
        label.textContent = selected.textContent.trim();
      }
    }

    function close(returnFocus = false) {
      menu.hidden = true;
      trigger.setAttribute("aria-expanded", "false");

      if (returnFocus) {
        trigger.focus();
      }
    }

    const controller = {
      root,
      trigger,
      close,
      updateLabel
    };

    function open() {
      dropdownControls.forEach(control => {
        if (control !== controller) {
          control.close();
        }
      });

      menu.hidden = false;
      trigger.setAttribute("aria-expanded", "true");

      const selected = getOptions().find(
        option => getValue(option) === input.value
      );

      (selected || getOptions()[0])?.focus();
    }

    trigger.addEventListener("click", () => {
      if (menu.hidden) {
        open();
      } else {
        close();
      }
    });

    menu.addEventListener("click", event => {
      const option = event.target.closest(
        `button[${optionAttribute}]`
      );

      if (!option) {
        return;
      }

      const value = getValue(option);

      close(true);

      /*
       * 직접 설정은 달력에서 확인한 뒤 적용합니다.
       * false를 반환하면 현재 필터 값을 변경하지 않습니다.
       */
      if (onSelect && onSelect(value) === false) {
        return;
      }

      input.value = value;
      updateLabel();
    });

    root.addEventListener("keydown", event => {
      if (event.key === "Escape" && !menu.hidden) {
        event.preventDefault();
        close(true);
      }

      if (
        event.target === trigger &&
        event.key === "ArrowDown"
      ) {
        event.preventDefault();
        open();
      }
    });

    root.addEventListener("focusout", event => {
      if (!root.contains(event.relatedTarget)) {
        close();
      }
    });

    close();
    updateLabel();
    dropdownControls.push(controller);

    return controller;
  }

  /* 모든 버튼형 필터를 연결합니다. */
  function setupDropdowns() {
    periodControl = createDropdown({
      root: document.querySelector("#period-dropdown"),
      trigger: document.querySelector("#period-button"),
      menu: document.querySelector("#period-options"),
      input: periodFilter,
      label: document.querySelector("#period-label"),
      optionAttribute: "data-period",

      onSelect(value) {
        if (value === "custom") {
          openCalendar();
          return false;
        }

        return true;
      }
    });

    document.querySelectorAll(".choice-dropdown").forEach(root => {
      createDropdown({
        root,
        trigger: root.querySelector(".choice-button"),
        menu: root.querySelector(".choice-options"),
        input: root.querySelector("input[type='hidden']"),
        label: root.querySelector(".choice-label"),
        optionAttribute: "data-value"
      });
    });

    document.addEventListener("click", event => {
      dropdownControls.forEach(control => {
        if (!control.root.contains(event.target)) {
          control.close();
        }
      });
    });
  }

  /* ==================================================
     표 출력
     ================================================== */

  /* 텍스트 셀을 만듭니다. */
  function createCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value;

    return cell;
  }

  /* 표 안내 메시지 */
  function showTableMessage(message) {
    const row = document.createElement("tr");
    const cell = createCell(message);

    cell.colSpan = 7;
    cell.className = "empty-message";

    row.append(cell);
    recordList.replaceChildren(row);
    resultMessage.textContent = message;
  }

  /* 검색 결과를 표에 표시합니다. */
  function renderRecords(records) {
    if (records.length === 0) {
      showTableMessage(
        "검색 조건에 맞는 조치 기록이 없습니다."
      );
      return;
    }

    const rows = document.createDocumentFragment();

    records.forEach(record => {
      const row = document.createElement("tr");

      if (record.status === "완료") {
        row.className = "completed-row";
      }

      const typeCell = document.createElement("td");
      const badge = document.createElement("span");

      badge.className = "alert-badge";

      if (record.type === "낙상 감지") {
        badge.classList.add("fall-badge");
      } else if (record.type === "침대 이탈") {
        badge.classList.add("bed-exit-badge");
      }

      badge.textContent = `● ${record.type}`;
      typeCell.append(badge);

      row.append(
        createCell(formatTime(record.occurredAt)),
        createCell(record.room),
        createCell(record.patient),
        typeCell,
        createCell(record.staff),
        createCell(record.status),
        createCell(formatTime(record.completedAt))
      );

      rows.append(row);
    });

    recordList.replaceChildren(rows);

    resultMessage.textContent =
      `총 ${records.length}건의 조치 기록이 검색되었습니다.`;
  }

  /* ==================================================
     검색
     ================================================== */

  /* 현재 필터 값을 읽습니다. */
  function readSearchConditions() {
    const formData = new FormData(searchForm);

    return {
      room: String(formData.get("room") || "all"),
      status: String(formData.get("status") || "all"),
      target: String(formData.get("target") || "all"),
      keyword: String(formData.get("keyword") || "")
        .trim()
        .toLocaleLowerCase()
    };
  }

  /* 병실·상태·이름 조건을 확인합니다. */
  function matchesConditions(record, conditions) {
    let names = "";

    if (conditions.target === "patient") {
      names = record.patient;
    } else if (conditions.target === "staff") {
      names = record.staff;
    } else {
      names = `${record.patient} ${record.staff}`;
    }

    return (
      (
        conditions.room === "all" ||
        record.room === conditions.room
      ) &&
      (
        conditions.status === "all" ||
        record.status === conditions.status
      ) &&
      names
        .toLocaleLowerCase()
        .includes(conditions.keyword)
    );
  }

  /*
   * 화면 필터를 적용합니다.
   * 선택하지 않은 필터는 all이므로 전체로 처리됩니다.
   */
  function applyFilters() {
    if (isLoading || loadFailed) {
      return;
    }

    const conditions = readSearchConditions();
    const period = periodFilter.value;

    const today = getReferenceDay();

    let startDay = "";
    let endDay = today;

    /*
     * 직접 설정을 선택한 경우에만
     * 시작일과 종료일을 검사합니다.
     */
    if (period === "custom") {
      if (!selectedStart || !selectedEnd) {
        return;
      }

      startDay = selectedStart;
      endDay = selectedEnd;
    } else if (period !== "all") {
      /*
       * 최근 7일 또는 최근 30일입니다.
       */
      startDay = getPeriodStart(
        endDay,
        Number(period)
      );
    }

    filteredRecords = allRecords.filter(record => {
      const recordDate = record.occurredAt.slice(0, 10);

      const matchesPeriod =
        period === "all" ||
        (
          recordDate >= startDay &&
          recordDate <= endDay
        );

      return (
        matchesPeriod &&
        matchesConditions(record, conditions)
      );
    });

    renderRecords(filteredRecords);

    /* 검색 후 스크롤을 맨 위로 이동합니다. */
    tableScroll.scrollTop = 0;
  }

  /* ==================================================
     기간 달력
     ================================================== */

  /* 달력에서 선택한 날짜를 표시합니다. */
  function updateCalendarSelection() {
    calendarDays
      .querySelectorAll("button")
      .forEach(button => {
        const date = button.dataset.date;

        const isEndpoint =
          date === draftStart ||
          date === draftEnd;

        const isInsideRange =
          draftEnd &&
          date > draftStart &&
          date < draftEnd;

        button.setAttribute(
          "aria-pressed",
          String(isEndpoint)
        );

        button.classList.toggle(
          "in-range",
          Boolean(isInsideRange)
        );
      });

    confirmCalendarButton.disabled =
      !draftStart || !draftEnd;

    if (!draftStart) {
      calendarHelp.textContent =
        "시작일과 종료일을 차례로 선택하세요.";
    } else if (!draftEnd) {
      calendarHelp.textContent =
        `${draftStart}부터 · 종료일을 선택하세요.`;
    } else {
      calendarHelp.textContent =
        `${draftStart} ~ ${draftEnd}`;
    }
  }

  /* 현재 달력의 날짜 버튼을 만듭니다. */
  function renderCalendar() {
    const year = displayedMonth.getFullYear();
    const month = displayedMonth.getMonth();

    calendarMonth.textContent =
      `${year}년 ${month + 1}월`;

    calendarDays.replaceChildren();

    const firstWeekday =
      new Date(year, month, 1).getDay();

    const lastDay =
      new Date(year, month + 1, 0).getDate();

    /* 월 시작 전 빈칸 */
    for (let i = 0; i < firstWeekday; i++) {
      const blank = document.createElement("span");

      blank.setAttribute("aria-hidden", "true");
      calendarDays.append(blank);
    }

    /* 날짜 버튼 */
    for (let day = 1; day <= lastDay; day++) {
      const key = dateKey(
        new Date(year, month, day)
      );

      const button = document.createElement("button");

      button.type = "button";
      button.textContent = day;
      button.dataset.date = key;

      button.setAttribute(
        "aria-label",
        `${year}년 ${month + 1}월 ${day}일`
      );

      button.addEventListener("click", () => {
        if (!draftStart || draftEnd) {
          draftStart = key;
          draftEnd = "";
        } else {
          [draftStart, draftEnd] =
            [draftStart, key].sort();
        }

        updateCalendarSelection();
      });

      calendarDays.append(button);
    }

    updateCalendarSelection();
  }

  /* 직접 설정 달력을 엽니다. */
  function openCalendar() {
    if (calendarDialog.open) {
      return;
    }

    draftStart = selectedStart;
    draftEnd = selectedEnd;

    const initialDate =
      selectedStart || getKoreanToday();

    const [year, month] =
      initialDate.split("-").map(Number);

    displayedMonth = new Date(
      year,
      month - 1,
      1
    );

    renderCalendar();
    calendarDialog.showModal();
  }

  /* 달력 취소 */
  function cancelCalendar() {
    calendarDialog.close();
    periodControl.trigger.focus();
  }

  /* 달력 확인 */
  function confirmCalendar() {
    if (!draftStart || !draftEnd) {
      return;
    }

    selectedStart = draftStart;
    selectedEnd = draftEnd;

    periodFilter.value = "custom";
    periodControl.updateLabel();

    calendarDialog.close();
    periodControl.trigger.focus();

    applyFilters();
  }

  /* ==================================================
     내보내기
     ================================================== */

  /* 현재 선택한 파일 형식을 반환합니다. */
  function getExportFormat() {
    return exportForm.querySelector(
      'input[name="format"]:checked'
    ).value;
  }

  /*
   * 내보내기 팝업을 엽니다.
   * 현재 화면의 filteredRecords만 사용합니다.
   */
  function openExportDialog() {
    if (isLoading || loadFailed) {
      window.alert(
        "기록을 정상적으로 불러온 뒤 다시 시도해주세요."
      );
      return;
    }

    /*
     * 검색 버튼을 누르지 않았더라도
     * 현재 선택된 필터를 바로 적용합니다.
     */
    applyFilters();

    exportCount.textContent =
      `현재 필터 결과 ${filteredRecords.length}건`;

    exportError.textContent = "";

    if (getExportFormat() === "pdf") {
      exportNote.textContent =
        "보고서 창에서 인쇄 후 PDF로 저장하세요.";
    } else {
      exportNote.textContent =
        "현재 선택한 필터 결과만 내보냅니다.";
    }

    exportSubmit.disabled =
      filteredRecords.length === 0;

    exportDialog.showModal();
  }

  /* CSV 특수문자 처리 */
  function escapeCsv(value) {
    let text = String(value ?? "");

    if (/^\s*[=+@-]/.test(text)) {
      text = `'${text}`;
    }

    return `"${text.replaceAll('"', '""')}"`;
  }

  /* 파일에 공통으로 사용할 행 데이터 */
  function makeExportRows(records) {
    return [
      [
        "발생 시각",
        "병실",
        "환자명",
        "알림 유형",
        "담당자",
        "처리 상태",
        "완료 시각"
      ],
      ...records.map(record => [
        record.occurredAt.replace("T", " "),
        record.room,
        record.patient,
        record.type,
        record.staff,
        record.status,
        record.completedAt
          ? record.completedAt.replace("T", " ")
          : ""
      ])
    ];
  }

  /* CSV 다운로드 */
  function downloadCsv(records) {
    const csv = "\uFEFF" + makeExportRows(records)
      .map(row => row.map(escapeCsv).join(","))
      .join("\r\n");

    const blob = new Blob([csv], {
      type: "text/csv;charset=utf-8;"
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = "조치기록.csv";

    document.body.append(link);
    link.click();
    link.remove();

    setTimeout(() => {
      URL.revokeObjectURL(url);
    }, 1000);
  }

  /* Excel 다운로드 */
  function downloadExcel(records) {
    if (!window.XLSX) {
      throw new Error(
        "Excel 라이브러리를 불러오지 못했습니다."
      );
    }

    const worksheet = window.XLSX.utils.aoa_to_sheet(
      makeExportRows(records)
    );

    worksheet["!cols"] = [
      { wch: 20 },
      { wch: 10 },
      { wch: 12 },
      { wch: 14 },
      { wch: 12 },
      { wch: 12 },
      { wch: 20 }
    ];

    const workbook = window.XLSX.utils.book_new();

    window.XLSX.utils.book_append_sheet(
      workbook,
      worksheet,
      "조치 기록"
    );

    window.XLSX.writeFile(
      workbook,
      "조치기록.xlsx"
    );
  }

  /* PDF 보고서 창 */
  function openPdfReport(records) {
    const reportWindow = window.open(
      "",
      "_blank",
      "width=1100,height=800"
    );

    if (!reportWindow) {
      throw new Error(
        "팝업이 차단되었습니다."
      );
    }

    const doc = reportWindow.document;

    doc.documentElement.lang = "ko";
    doc.head.replaceChildren();
    doc.body.replaceChildren();

    const title = doc.createElement("title");
    title.textContent = "조치기록 보고서";

    const charset = doc.createElement("meta");
    charset.setAttribute("charset", "UTF-8");

    const style = doc.createElement("style");

    style.textContent = `
      body {
        margin: 28px;
        color: #263a30;
        font-family: "Malgun Gothic", sans-serif;
        font-size: 12px;
      }

      h1 {
        font-size: 22px;
      }

      table {
        width: 100%;
        margin-top: 20px;
        border-collapse: collapse;
      }

      th,
      td {
        padding: 8px;
        border: 1px solid #bbc7bf;
        text-align: center;
      }

      th {
        background: #e7eee8;
      }

      button {
        padding: 10px 16px;
        border: 0;
        border-radius: 6px;
        background: #3b574a;
        color: white;
      }

      @page {
        size: A4 landscape;
        margin: 12mm;
      }

      @media print {
        .print-controls {
          display: none;
        }
      }
    `;

    doc.head.append(charset, title, style);

    const controls = doc.createElement("div");
    controls.className = "print-controls";

    const printButton = doc.createElement("button");
    printButton.textContent = "인쇄 / PDF로 저장";
    printButton.addEventListener("click", () => {
      reportWindow.print();
    });

    controls.append(printButton);

    const heading = doc.createElement("h1");
    heading.textContent = "안전 조치 기록 보고서";

    const count = doc.createElement("p");
    count.textContent =
      `현재 필터 결과 ${records.length}건`;

    const table = doc.createElement("table");
    const thead = doc.createElement("thead");
    const tbody = doc.createElement("tbody");

    makeExportRows(records).forEach((values, index) => {
      const row = doc.createElement("tr");

      values.forEach(value => {
        const cell = doc.createElement(
          index === 0 ? "th" : "td"
        );

        cell.textContent = value;
        row.append(cell);
      });

      if (index === 0) {
        thead.append(row);
      } else {
        tbody.append(row);
      }
    });

    table.append(thead, tbody);
    doc.body.append(
      controls,
      heading,
      count,
      table
    );

    reportWindow.focus();
    reportWindow.print();
  }

  /* 파일 형식에 따라 현재 결과를 저장합니다. */
  function submitExport(event) {
    event.preventDefault();

    if (!filteredRecords.length) {
      exportError.textContent =
        "현재 필터 조건에 해당하는 기록이 없습니다.";
      return;
    }

    const format = getExportFormat();

    try {
      if (format === "xlsx") {
        downloadExcel(filteredRecords);
      } else if (format === "csv") {
        downloadCsv(filteredRecords);
      } else {
        openPdfReport(filteredRecords);
      }

      exportDialog.close();
      exportButton.focus();
    } catch (error) {
      exportError.textContent = error.message;
    }
  }

  /* ==================================================
     서버 데이터 조회
     ================================================== */

  async function loadRecords() {
    isLoading = true;
    loadFailed = false;

    showTableMessage(
      "조치 기록을 불러오는 중입니다."
    );

    try {
      let records = demoRecords;

      if (!settings.useDemoData) {
        const response = await fetch(
          historyPage.dataset.apiUrl,
          {
            credentials: "same-origin",
            headers: {
              Accept: "application/json"
            },
            cache: "no-store"
          }
        );

        if (!response.ok) {
          throw new Error(
            `기록 조회 실패: ${response.status}`
          );
        }

        records = await response.json();
      }

      const requiredFields = [
        "occurredAt",
        "room",
        "patient",
        "type",
        "staff",
        "status",
        "completedAt"
      ];

      const validResponse =
        Array.isArray(records) &&
        records.every(record =>
          record &&
          requiredFields.every(field =>
            typeof record[field] === "string"
          )
        );

      if (!validResponse) {
        throw new Error(
          "서버 응답 형식을 확인해주세요."
        );
      }

      allRecords = [...records].sort((a, b) =>
        b.occurredAt.localeCompare(a.occurredAt)
      );

      isLoading = false;
      applyFilters();
    } catch (error) {
      isLoading = false;
      loadFailed = true;
      allRecords = [];
      filteredRecords = [];

      showTableMessage(
        "기록을 불러오지 못했습니다."
      );

      console.error(error);
    }
  }

  /* ==================================================
     이벤트 연결
     ================================================== */

  /* 검색 버튼 또는 Enter */
  searchForm.addEventListener("submit", event => {
    event.preventDefault();
    applyFilters();
  });

  /* 달력 이동 */
  previousMonthButton.addEventListener("click", () => {
    displayedMonth.setMonth(
      displayedMonth.getMonth() - 1
    );

    renderCalendar();
  });

  nextMonthButton.addEventListener("click", () => {
    displayedMonth.setMonth(
      displayedMonth.getMonth() + 1
    );

    renderCalendar();
  });

  /* 달력 취소·확인 */
  cancelCalendarButton.addEventListener(
    "click",
    cancelCalendar
  );

  confirmCalendarButton.addEventListener(
    "click",
    confirmCalendar
  );

  calendarDialog.addEventListener("cancel", event => {
    event.preventDefault();
    cancelCalendar();
  });

  /* 내보내기 팝업 */
  exportButton.addEventListener(
    "click",
    openExportDialog
  );

  exportForm.addEventListener(
    "submit",
    submitExport
  );

  /* 파일 형식 변경 시 안내 문구 변경 */
  exportForm
    .querySelectorAll('input[name="format"]')
    .forEach(input => {
      input.addEventListener("change", () => {
        if (input.checked) {
          exportNote.textContent =
            input.value === "pdf"
              ? "보고서 창에서 PDF로 저장하세요."
              : "현재 선택한 필터 결과만 내보냅니다.";
        }
      });
    });

  /* 팝업 닫기 */
  function closeExportDialog() {
    exportDialog.close();
    exportButton.focus();
  }

  document
    .querySelector("#export-close")
    .addEventListener(
      "click",
      closeExportDialog
    );

  document
    .querySelector("#export-cancel")
    .addEventListener(
      "click",
      closeExportDialog
    );

  exportDialog.addEventListener("cancel", event => {
    event.preventDefault();
    closeExportDialog();
  });

  /* 로그아웃 */
  // logoutButton.addEventListener("click", () => {
  //   window.alert(
  //     "로그아웃은 Spring Boot 인증 기능과 연결해주세요."
  //   );
  // });

  /* ==================================================
     초기 실행
     ================================================== */

  renderRoomOptions();
  setupDropdowns();

  updateClock();
  setInterval(updateClock, 30000);

  loadRecords();
}