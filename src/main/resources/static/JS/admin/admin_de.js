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

    const parts = new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul",
        year: "numeric",
        month: "numeric",
        day: "numeric",
        weekday: "long",
        hour: "2-digit",
        minute: "2-digit",
        hourCycle: "h23"
    }).formatToParts(now);

    const values = Object.fromEntries(
        parts.map(part => [part.type, part.value])
    );

    adminClock.dateTime = now.toISOString();
    adminClock.textContent =
        `${values.year}년 ${values.month}월 ${values.day}일 ${values.weekday} `
        + `${values.hour}:${values.minute}`;
}

/* INACTIVE 사용자만 DB에서 읽고, 변경 성공 후 목록을 다시 조회한다. */
const adminInactiveSearch = document.querySelector("#admin-inactive-search");
const adminInactiveWard = document.querySelector("#admin-inactive-ward");
const adminInactiveSelectAll = document.querySelector("#admin-inactive-select-all");
const adminBulkButtons = Array.from(document.querySelectorAll("[data-bulk-status]"));
const adminRows = document.querySelector("#admin-inactive-rows");
const adminList = document.querySelector("#admin-inactive-list");
const adminStatusDialog = document.querySelector("#admin-status-dialog");
const adminStatusConfirm = document.querySelector("#admin-status-confirm");
const adminStatusCancel = document.querySelector("#admin-status-cancel");
const adminError = document.querySelector("#admin-inactive-error");
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content ?? "";
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
let adminInactiveUsers = [];
let adminSelectedIds = new Set();
let adminPendingStatusChange = null;
let adminBusy = false;
let adminLoading = false;
let adminLoadFailed = false;

async function requestAdminInactiveApi(url, options = {}) {
    const headers = new Headers(options.headers ?? {});
    headers.set("Accept", "application/json");
    if (csrfToken) headers.set(csrfHeader, csrfToken);
    const response = await fetch(url, { ...options, headers, cache: "no-store" });
    if (response.status === 401 || response.redirected) {
        throw new Error("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    }
    const contentType = response.headers.get("content-type") || "";
    const data = contentType.includes("application/json") ? await response.json() : null;
    if (!response.ok) {
        throw new Error(data?.message || (response.status === 403
            ? "요청 권한 또는 병원 접속 정보를 확인해 주세요."
            : "요청 처리에 실패했습니다. (" + response.status + ")"));
    }
    if (!contentType.includes("application/json") && response.status !== 204) {
        throw new Error("서버 응답을 확인할 수 없습니다. 다시 로그인해 주세요.");
    }
    return data;
}

function showAdminError(message) {
    document.querySelector("#admin-inactive-error-message").textContent = message;
    adminError.hidden = false;
}

function getAdminVisibleUsers() {
    const query = adminInactiveSearch.value.trim().toLowerCase();
    const wardId = adminInactiveWard.value;
    return adminInactiveUsers.filter(user =>
        (user.userName + " " + user.userId).toLowerCase().includes(query)
        && (!wardId || String(user.wardId ?? "UNASSIGNED") === wardId));
}

function getAdminSelectedUsers() {
    return getAdminVisibleUsers().filter(user => adminSelectedIds.has(user.userId));
}

function updateAdminInactiveSelection() {
    const visibleUsers = getAdminVisibleUsers();
    const selectedCount = getAdminSelectedUsers().length;
    const disabled = adminBusy || adminLoading || adminLoadFailed;
    adminInactiveSelectAll.checked = visibleUsers.length > 0 && selectedCount === visibleUsers.length;
    adminInactiveSelectAll.indeterminate = false;
    adminInactiveSelectAll.disabled = disabled || !visibleUsers.length;
    document.querySelector("#admin-bulk-count").textContent = "현재 목록에서 " + selectedCount + "명 선택";
    document.querySelector(".admin-bulk-actions").dataset.hasSelection = String(selectedCount > 0);
    for (const button of adminBulkButtons) {
        button.disabled = disabled || !selectedCount;
    }
    for (const input of adminRows.querySelectorAll("input, select")) input.disabled = disabled;
    adminInactiveSearch.disabled = adminBusy || adminLoading;
    adminInactiveWard.disabled = adminBusy || adminLoading;
    document.querySelector("#admin-inactive-retry").disabled = adminBusy || adminLoading;
    adminList.setAttribute("aria-busy", String(adminBusy || adminLoading));
}

function formatAdminDeactivatedDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";
    return new Intl.DateTimeFormat("sv-SE", {
        timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit"
    }).format(date).replaceAll("-", ".");
}

function renderAdminInactiveUsers() {
    adminRows.replaceChildren();
    const users = getAdminVisibleUsers();
    for (const user of users) {
        const row = document.createElement("tr");
        row.dataset.userId = user.userId;
        const checkCell = document.createElement("td");
        const label = document.createElement("label");
        label.className = "admin-inactive-selection";
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.className = "admin-inactive-checkbox";
        checkbox.value = user.userId;
        checkbox.checked = adminSelectedIds.has(user.userId);
        checkbox.setAttribute("aria-label", user.userName + " 선택");
        checkbox.addEventListener("change", () => {
            if (checkbox.checked) adminSelectedIds.add(user.userId);
            else adminSelectedIds.delete(user.userId);
            updateAdminInactiveSelection();
        });
        label.append(checkbox);
        checkCell.append(label);
        row.append(checkCell);
        for (const value of [user.userName, user.userId, user.wardName || "미배정", formatAdminDeactivatedDate(user.deactivatedAt)]) {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.append(cell);
        }
        const statusCell = document.createElement("td");
        const select = document.createElement("select");
        select.className = "admin-inactive-action";
        select.dataset.status = "INACTIVE";
        select.setAttribute("aria-label", user.userName + " 계정 관리");
        for (const [value, text] of [["INACTIVE", "비활성화"], ["ACTIVATE", "활성화"], ["DELETE", "삭제"]]) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = text;
            select.append(option);
        }
        select.addEventListener("change", () => {
            const nextStatus = select.value;
            select.value = "INACTIVE";
            if (nextStatus !== "INACTIVE") openAdminStatusChange([user], nextStatus, select);
        });
        statusCell.append(select);
        row.append(statusCell);
        adminRows.append(row);
    }
    document.querySelector("#admin-inactive-count").innerHTML = adminLoadFailed
        ? "—<span>명</span>" : adminInactiveUsers.length + "<span>명</span>";
    document.querySelector("#admin-inactive-result").textContent = adminLoading
        ? "사용자 목록을 불러오는 중입니다."
        : adminLoadFailed ? "목록을 불러오지 못했습니다." : "검색 결과 " + users.length + "명";
    const empty = document.querySelector("#admin-inactive-empty");
    empty.hidden = adminLoading || adminLoadFailed || users.length > 0;
    const filtered = Boolean(adminInactiveSearch.value.trim() || adminInactiveWard.value);
    document.querySelector("#admin-inactive-empty-title").textContent = filtered
        ? "검색 결과가 없습니다" : "비활성화 사용자가 없습니다";
    document.querySelector("#admin-inactive-empty-description").textContent = filtered
        ? "이름, 아이디 또는 병동 조건을 다시 확인해 주세요."
        : "승인완료 목록에서 비활성화한 사용자가 여기에 표시됩니다.";
    document.querySelector("#admin-inactive-reset").hidden = !filtered;
    updateAdminInactiveSelection();
}

async function loadAdminInactiveData() {
    adminLoading = true;
    adminLoadFailed = false;
    adminError.hidden = true;
    renderAdminInactiveUsers();
    try {
        if (!csrfToken) throw new Error("DB 목록은 Spring Boot 서버에 로그인한 뒤 확인해 주세요.");
        const [users, wards] = await Promise.all([
            requestAdminInactiveApi("/api/admin/users/inactive"),
            requestAdminInactiveApi("/api/admin/wards")
        ]);
        if (!Array.isArray(users) || !Array.isArray(wards)) throw new Error("목록 응답 형식이 올바르지 않습니다.");
        adminInactiveUsers = users.filter(user => user.authStatus === "INACTIVE");
        const previousWard = adminInactiveWard.value;
        adminInactiveWard.replaceChildren();
        for (const [value, text] of [
            ["", "전체 병동"], ...wards.map(ward => [String(ward.wardId), ward.wardName]), ["UNASSIGNED", "미배정"]
        ]) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = text;
            adminInactiveWard.append(option);
        }
        adminInactiveWard.value = previousWard;
        if (!adminInactiveWard.value) adminInactiveWard.value = "";
        adminSelectedIds = new Set([...adminSelectedIds].filter(id => adminInactiveUsers.some(user => user.userId === id)));
        return true;
    } catch (error) {
        adminInactiveUsers = [];
        adminSelectedIds.clear();
        adminLoadFailed = true;
        showAdminError(error.message || "목록을 불러오지 못했습니다.");
        return false;
    } finally {
        adminLoading = false;
        renderAdminInactiveUsers();
    }
}

function openAdminStatusChange(users, nextStatus, trigger, bulk = false) {
    if (!users.length || adminBusy || adminLoading || adminLoadFailed) return;
    if (nextStatus === "INACTIVE") {
        showAdminFeedback("선택한 사용자는 이미 비활성화 상태입니다.");
        return;
    }
    if (!["ACTIVATE", "DELETE"].includes(nextStatus)) return;
    const isDelete = nextStatus === "DELETE";
    const subject = bulk ? "선택한 사용자 " + users.length + "명" : users[0].userName + "님";
    adminPendingStatusChange = { users: [...users], nextStatus, trigger, subject };
    document.querySelector("#admin-status-dialog-title").textContent = isDelete ? "사용자 삭제" : "상태 변경";
    document.querySelector("#admin-status-dialog-description").textContent = isDelete
        ? subject + "의 계정을 삭제하시겠습니까?"
        : subject + "의 상태를 활성화로 바꾸시겠습니까?";
    document.querySelector("#admin-status-dialog-note").textContent = isDelete
        ? "계정이 DB에서 영구 삭제되며 복구할 수 없습니다."
        : "승인완료 상태로 복귀하며, 이전 배정 병동은 유지됩니다.";
    adminStatusConfirm.classList.toggle("admin-delete-confirm", isDelete);
    adminStatusDialog.showModal();
}

async function submitAdminStatusChange(event) {
    event.preventDefault();
    if (!adminPendingStatusChange || adminBusy) return;
    const change = adminPendingStatusChange;
    adminBusy = true;
    adminStatusConfirm.disabled = true;
    adminStatusCancel.disabled = true;
    adminStatusConfirm.textContent = "처리 중…";
    updateAdminInactiveSelection();
    const failures = [];
    let succeeded = 0;
    try {
        // 각 계정의 결과를 구분하여 일부 실패를 전체 성공으로 표시하지 않는다.
        for (const user of change.users) {
            try {
                const action = change.nextStatus === "DELETE" ? "inactive" : "activate";
                await requestAdminInactiveApi("/api/admin/users/" + encodeURIComponent(user.userId) + "/" + action, {
                    method: change.nextStatus === "DELETE" ? "DELETE" : "PATCH"
                });
                succeeded += 1;
                adminSelectedIds.delete(user.userId);
            } catch (error) {
                failures.push(user.userName + " (" + user.userId + "): " + error.message);
            }
        }
        adminPendingStatusChange = null;
        adminStatusDialog.close();
        if (!failures.length && change.nextStatus === "ACTIVATE") {
            window.location.assign(document.querySelector("#admin-settings-back").href);
            return;
        }
        const loaded = await loadAdminInactiveData();
        if (failures.length) {
            showAdminError(succeeded + "명 처리 완료 / " + failures.length + "명 실패\n" + failures.join("\n")
                + (loaded ? "" : "\n최신 목록도 불러오지 못했습니다. 다시 불러오기를 눌러 주세요."));
        }
        if (succeeded) {
            showAdminFeedback(succeeded + "명의 계정을 " + (change.nextStatus === "DELETE" ? "삭제" : "활성화") + "했습니다.");
        }
        adminInactiveSearch.focus();
    } finally {
        adminBusy = false;
        adminStatusConfirm.disabled = false;
        adminStatusCancel.disabled = false;
        adminStatusConfirm.textContent = "확인";
        updateAdminInactiveSelection();
    }
}

adminInactiveSelectAll.addEventListener("change", () => {
    for (const user of getAdminVisibleUsers()) {
        if (adminInactiveSelectAll.checked) adminSelectedIds.add(user.userId);
        else adminSelectedIds.delete(user.userId);
    }
    renderAdminInactiveUsers();
});
for (const button of adminBulkButtons) {
    button.addEventListener("click", () => openAdminStatusChange(
        getAdminSelectedUsers(), button.dataset.bulkStatus, button, true
    ));
}
document.querySelector("#admin-status-form").addEventListener("submit", submitAdminStatusChange);
adminStatusCancel.addEventListener("click", () => adminStatusDialog.close());
adminStatusDialog.addEventListener("cancel", event => {
    if (adminBusy) event.preventDefault();
});
adminStatusDialog.addEventListener("close", () => {
    const change = adminPendingStatusChange;
    adminPendingStatusChange = null;
    change?.trigger.focus();
});
document.querySelector("#admin-inactive-search-form").addEventListener("submit", event => {
    event.preventDefault();
    if (!adminBusy) renderAdminInactiveUsers();
});
adminInactiveSearch.addEventListener("input", renderAdminInactiveUsers);
adminInactiveWard.addEventListener("change", renderAdminInactiveUsers);
document.querySelector("#admin-inactive-reset").addEventListener("click", () => {
    if (adminBusy || adminLoading) return;
    adminInactiveSearch.value = "";
    adminInactiveWard.value = "";
    renderAdminInactiveUsers();
    adminInactiveSearch.focus();
});
document.querySelector("#admin-inactive-retry").addEventListener("click", loadAdminInactiveData);

updateAdminClock();
setInterval(updateAdminClock, 1000);
loadAdminInactiveData();
