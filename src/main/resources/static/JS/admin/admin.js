"use strict";

let adminUsers = [];
let adminActiveStatus = "PENDING";
let adminSearchQuery = "";
let adminSelectedUser = null;
let adminAction = "";
let adminToastTimer;
const adminRows = document.querySelector("#admin-user-rows");
const adminTabs = Array.from(document.querySelectorAll("[data-status]"));
const adminDialog = document.querySelector("#admin-action-dialog");
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

function requestHeaders() {
    return csrfToken && csrfHeader
        ? { "Accept": "application/json", [csrfHeader]: csrfToken }
        : { "Accept": "application/json" };
}

function showAdminFeedback(message) {
    const feedback = document.querySelector("#admin-feedback");
    clearTimeout(adminToastTimer);
    feedback.textContent = message;
    feedback.hidden = false;
    adminToastTimer = setTimeout(() => { feedback.hidden = true; }, 4000);
}

function renderAdminUsers() {
    const currentUsers = adminUsers;
    document.querySelector("#admin-total-count").textContent = `${currentUsers.length}명`;
    document.querySelector("#admin-approved-count").textContent = `${currentUsers.filter(user => user.status === "APPROVED").length}명`;
    document.querySelector("#admin-pending-count").textContent = `${currentUsers.filter(user => user.status === "PENDING").length}명`;
    const visibleUsers = currentUsers.filter(user => user.status === adminActiveStatus &&
        `${user.name} ${user.userId}`.toLowerCase().includes(adminSearchQuery));
    adminRows.replaceChildren();
    for (const user of visibleUsers) {
        const row = document.createElement("tr");
        for (const value of [user.name, user.userId, user.ward]) {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.append(cell);
        }
        const actionsCell = document.createElement("td");
        const actions = document.createElement("div");
        actions.className = "admin-row-actions";
        const userActions = user.status === "PENDING"
            ? [["APPROVE", "✓ 승인", "admin-approve"], ["REJECT", "× 반려", "admin-reject"]]
            : [];
        for (const [action, label, className] of userActions) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = className;
            button.textContent = label;
            button.setAttribute("aria-label", `${user.name} ${label.replace(/[✓×]/g, "").trim()}`);
            button.addEventListener("click", () => openAdminAction(user, action));
            actions.append(button);
        }
        actionsCell.append(actions);
        row.append(actionsCell);
        adminRows.append(row);
    }
    document.querySelector("#admin-empty").hidden = visibleUsers.length > 0;
}

function selectAdminTab(tab) {
    adminActiveStatus = tab.dataset.status;
    for (const item of adminTabs) {
        const selected = item === tab;
        item.setAttribute("aria-selected", String(selected));
        item.tabIndex = selected ? 0 : -1;
    }
    document.querySelector("#admin-list").setAttribute("aria-labelledby", tab.id);
    renderAdminUsers();
}

for (const tab of adminTabs) {
    tab.addEventListener("click", () => selectAdminTab(tab));
    tab.addEventListener("keydown", event => {
        if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
        event.preventDefault();
        const nextTab = event.key === "Home" ? adminTabs[0] : event.key === "End" ? adminTabs[1] : adminTabs.find(item => item !== tab);
        selectAdminTab(nextTab);
        nextTab.focus();
    });
}

document.querySelector("#admin-search-form").addEventListener("submit", event => {
    event.preventDefault();
    adminSearchQuery = document.querySelector("#admin-search-input").value.trim().toLowerCase();
    renderAdminUsers();
});
document.querySelector("#admin-search-input").addEventListener("input", event => {
    if (!event.target.value) {
        adminSearchQuery = "";
        renderAdminUsers();
    }
});

function openAdminAction(user, action) {
    adminSelectedUser = user;
    adminAction = action;
    const isReject = action === "REJECT";
    document.querySelector("#admin-dialog-title").textContent = isReject ? "가입 신청 반려" : "가입 신청 승인";
    document.querySelector("#admin-dialog-description").textContent = isReject
        ? `${user.name}님의 가입 신청을 반려하시겠습니까?`
        : `${user.name}님의 가입 신청을 승인하시겠습니까?`;
    document.querySelector("#admin-dialog-confirm").textContent = isReject ? "반려하기" : "승인하기";
    adminDialog.showModal();
}

document.querySelector("#admin-dialog-cancel").addEventListener("click", () => adminDialog.close());
document.querySelector("#admin-action-form").addEventListener("submit", async event => {
    event.preventDefault();
    if (!adminSelectedUser) return;
    const confirmButton = document.querySelector("#admin-dialog-confirm");
    const isReject = adminAction === "REJECT";
    const actionPath = isReject ? "reject" : "approve";
    confirmButton.disabled = true;
    try {
        const response = await fetch(`/api/admin/users/${encodeURIComponent(adminSelectedUser.userId)}/${actionPath}`, {
            method: isReject ? "DELETE" : "PATCH",
            headers: requestHeaders()
        });
        if (!response.ok) throw new Error(`관리자 처리 실패: ${response.status}`);
        adminDialog.close();
        await loadAdminUsers(true);
        adminTabs.find(tab => tab.dataset.status === adminActiveStatus).focus();
        showAdminFeedback(isReject
            ? "가입 신청을 반려하고 계정을 삭제했습니다."
            : "가입 신청을 승인하고 권한을 부여했습니다.");
    } catch (error) {
        showAdminFeedback("처리 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
        confirmButton.disabled = false;
        adminSelectedUser = null;
    }
});

async function loadAdminUsers(silent = false) {
    try {
        const response = await fetch("/api/admin/users", {
            headers: { "Accept": "application/json" }
        });
        if (!response.ok) throw new Error(`사용자 목록 조회 실패: ${response.status}`);
        adminUsers = await response.json();
        renderAdminUsers();
    } catch (error) {
        adminUsers = [];
        renderAdminUsers();
        if (!silent) showAdminFeedback("사용자 목록을 불러오지 못했습니다.");
    }
}

document.querySelector("#admin-logout").addEventListener("click", async () => {
    const response = await fetch("/logout", { method: "POST", headers: requestHeaders() });
    if (response.ok || response.redirected) window.location.assign("/login?logout");
});

function updateAdminClock() {
    const now = new Date();
    const clock = document.querySelector("#admin-clock");
    const parts = new Intl.DateTimeFormat("en-GB", {
        timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23"
    }).formatToParts(now);
    const values = Object.fromEntries(parts.map(part => [part.type, part.value]));
    clock.dateTime = now.toISOString();
    clock.textContent = `${values.year}년 ${values.month}월 ${values.day}일 ${values.hour}:${values.minute}`;
}

loadAdminUsers();
updateAdminClock();
setInterval(updateAdminClock, 30000);
