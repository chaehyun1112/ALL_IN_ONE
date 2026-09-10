"use strict";

const adminFeedback = document.querySelector("#admin-feedback");
const adminClock = document.querySelector("#admin-clock");
let adminToastTimer = null;

function showAdminFeedback(message) {
    clearTimeout(adminToastTimer);

    adminFeedback.textContent = message;
    adminFeedback.hidden = false;

    adminToastTimer = setTimeout(() => {
        adminFeedback.hidden = true;
    }, 4000);
}

function updateAdminClock() {
    const now = new Date();

    const parts = new Intl.DateTimeFormat("en-GB", {
        timeZone: "Asia/Seoul",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hourCycle: "h23"
    }).formatToParts(now);

    const values = Object.fromEntries(
        parts.map(part => [part.type, part.value])
    );

    adminClock.dateTime = now.toISOString();
    adminClock.textContent =
        `${values.year}년 ${values.month}월 ${values.day}일 `
        + `${values.hour}:${values.minute}`;
}

/* 설정 화면 미리보기: 실제 계정 데이터 및 관리 API와 분리한다. */
const adminInactiveSearch = document.querySelector("#admin-inactive-search");
const adminInactiveWard = document.querySelector("#admin-inactive-ward");
const adminInactiveSelectAll = document.querySelector("#admin-inactive-select-all");
const adminBulkButtons = Array.from(document.querySelectorAll("[data-bulk-status]"));

function getAdminSelectedRows() {
    return Array.from(document.querySelectorAll("#admin-inactive-rows tr"))
        .filter(row => !row.hidden && row.querySelector(".admin-inactive-checkbox").checked);
}

function updateAdminInactiveSelection() {
    const visibleRows = Array.from(document.querySelectorAll("#admin-inactive-rows tr"))
        .filter(row => !row.hidden);
    const selectedCount = visibleRows.filter(row =>
        row.querySelector(".admin-inactive-checkbox").checked
    ).length;
    adminInactiveSelectAll.checked = visibleRows.length > 0 && selectedCount === visibleRows.length;
    adminInactiveSelectAll.indeterminate = false;
    adminInactiveSelectAll.disabled = visibleRows.length === 0;
    document.querySelector("#admin-bulk-count").textContent = `현재 목록에서 ${selectedCount}명 선택`;
    document.querySelector(".admin-bulk-actions").dataset.hasSelection = String(selectedCount > 0);
    for (const button of adminBulkButtons) {
        button.disabled = selectedCount === 0;
    }
}

adminInactiveSelectAll.addEventListener("change", () => {
    for (const row of document.querySelectorAll("#admin-inactive-rows tr")) {
        if (!row.hidden) {
            row.querySelector(".admin-inactive-checkbox").checked = adminInactiveSelectAll.checked;
        }
    }
    updateAdminInactiveSelection();
});
for (const checkbox of document.querySelectorAll(".admin-inactive-checkbox")) {
    checkbox.addEventListener("change", updateAdminInactiveSelection);
}

// 프론트 미리보기 전용: 실제 계정 변경 API는 호출하지 않는다.
const adminStatusDialog = document.querySelector("#admin-status-dialog");
const adminStatusConfirm = document.querySelector("#admin-status-confirm");
let adminPendingStatusChange = null;

function openAdminStatusChange(rows, nextStatus, trigger, bulk = false) {
    if (!rows.length) return;
    const isDelete = nextStatus === "DELETE";
    const statusLabel = nextStatus === "ACTIVATE" ? "활성화" : "비활성화";
    const subject = bulk ? `선택한 사용자 ${rows.length}명` : `${rows[0].cells[1].textContent}님`;
    adminPendingStatusChange = { rows, nextStatus, trigger, statusLabel, subject };
    document.querySelector("#admin-status-dialog-title").textContent = isDelete ? "사용자 삭제" : "상태 변경";
    document.querySelector("#admin-status-dialog-description").textContent = isDelete
        ? `${subject}을 목록에서 삭제하시겠습니까?`
        : `${subject}의 상태를 ${statusLabel}로 바꾸시겠습니까?`;
    document.querySelector("#admin-status-dialog-note").textContent = isDelete
        ? "삭제할 사용자와 계정 정보를 다시 확인해 주세요."
        : nextStatus === "ACTIVATE"
            ? "서비스 이용을 재개할 사용자인지 확인해 주세요."
            : "서비스 이용을 중지할 사용자인지 확인해 주세요.";
    adminStatusConfirm.classList.toggle("admin-delete-confirm", isDelete);
    adminStatusDialog.returnValue = "cancel";
    adminStatusDialog.showModal();
}

for (const button of adminBulkButtons) {
    button.addEventListener("click", () => {
        openAdminStatusChange(getAdminSelectedRows(), button.dataset.bulkStatus, button, true);
    });
}

for (const select of document.querySelectorAll(".admin-inactive-action")) {
    select.dataset.status = select.value;
    select.addEventListener("change", () => {
        const nextStatus = select.value;
        select.value = select.dataset.status;
        if (nextStatus === select.dataset.status) return;

        openAdminStatusChange([select.closest("tr")], nextStatus, select);
    });
}

adminStatusDialog.addEventListener("close", () => {
    const change = adminPendingStatusChange;
    adminPendingStatusChange = null;
    if (!change) return;
    if (adminStatusDialog.returnValue !== "confirm") {
        change.trigger.focus();
        return;
    }

    if (change.nextStatus === "DELETE") {
        for (const row of change.rows) row.remove();
        showAdminFeedback(`${change.subject}을 목록에서 삭제했습니다.`);
        adminInactiveSearch.focus();
    } else {
        for (const row of change.rows) {
            const select = row.querySelector(".admin-inactive-action");
            select.value = change.nextStatus;
            select.dataset.status = change.nextStatus;
        }
        showAdminFeedback(`${change.subject}의 상태를 ${change.statusLabel}로 변경했습니다.`);
        change.trigger.focus();
    }
    const inactiveCount = Array.from(document.querySelectorAll(".admin-inactive-action"))
        .filter(select => select.dataset.status === "INACTIVE").length;
    document.querySelector("#admin-inactive-count").innerHTML = `${inactiveCount}<span>명</span>`;
    filterAdminInactiveUsers();
});

function filterAdminInactiveUsers() {
    const query = adminInactiveSearch.value.trim().toLowerCase();
    const ward = adminInactiveWard.value;
    let count = 0;
    for (const row of document.querySelectorAll("#admin-inactive-rows tr")) {
        const text = `${row.cells[1].textContent} ${row.cells[2].textContent}`.toLowerCase();
        const matches = text.includes(query) && (!ward || row.dataset.ward === ward);
        row.hidden = !matches;
        if (matches) count += 1;
    }
    document.querySelector("#admin-inactive-result").textContent = `검색 결과 ${count}명`;
    document.querySelector("#admin-inactive-empty").hidden = count > 0;
    updateAdminInactiveSelection();
}

document.querySelector("#admin-inactive-search-form").addEventListener("submit", event => {
    event.preventDefault();
    filterAdminInactiveUsers();
});
adminInactiveSearch.addEventListener("input", filterAdminInactiveUsers);
adminInactiveWard.addEventListener("change", filterAdminInactiveUsers);
document.querySelector("#admin-inactive-reset").addEventListener("click", () => {
    adminInactiveSearch.value = "";
    adminInactiveWard.value = "";
    filterAdminInactiveUsers();
    adminInactiveSearch.focus();
});

updateAdminClock();
setInterval(updateAdminClock, 30000);
filterAdminInactiveUsers();
