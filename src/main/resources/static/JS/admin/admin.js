"use strict";

// 프런트엔드 시연 데이터입니다. 새로고침하면 초기화되며 실제 계정은 변경하지 않습니다.
const adminUsers = [
    { userId: "user.kim01", name: "김서윤", ward: "A병동", status: "PENDING" },
    { userId: "user.kang02", name: "강하은", ward: "C병동", status: "PENDING" },
    { userId: "user.park03", name: "박지영", ward: "B병동", status: "PENDING" },
    { userId: "user.lee04", name: "이하늘", ward: "D병동", status: "PENDING" },
    { userId: "user.choi05", name: "최유진", ward: "C병동", status: "PENDING" },
    { userId: "user.jung06", name: "정다은", ward: "F병동", status: "PENDING" }
];
let adminActiveStatus = "PENDING";
let adminSearchQuery = "";
let adminSelectedUser = null;
let adminAction = "";
let adminToastTimer;
const adminRows = document.querySelector("#admin-user-rows");
const adminTabs = Array.from(document.querySelectorAll("[data-status]"));
const adminDialog = document.querySelector("#admin-action-dialog");
const adminWardSelect = document.querySelector("#admin-ward-select");

function showAdminFeedback(message) {
    const feedback = document.querySelector("#admin-feedback");
    clearTimeout(adminToastTimer);
    feedback.textContent = message;
    feedback.hidden = false;
    adminToastTimer = setTimeout(() => { feedback.hidden = true; }, 4000);
}

function renderAdminUsers() {
    const currentUsers = adminUsers.filter(user => user.status !== "REJECTED");
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
            : [["ASSIGN", "병동 변경", "admin-approve"]];
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
    const isAssign = action === "ASSIGN";
    document.querySelector("#admin-dialog-title").textContent = isReject ? "가입 신청 반려" : isAssign ? "담당 병동 변경" : "가입 신청 승인";
    document.querySelector("#admin-dialog-description").textContent = isReject
        ? `${user.name}님의 가입 신청을 반려하시겠습니까?`
        : isAssign ? `${user.name} (${user.userId})님의 담당 병동을 선택해주세요.` : `${user.name}님의 가입 신청을 승인하시겠습니까?`;
    document.querySelector("#admin-ward-field").hidden = !isAssign;
    adminWardSelect.disabled = !isAssign;
    adminWardSelect.required = isAssign;
    adminWardSelect.value = user.ward;
    document.querySelector("#admin-dialog-confirm").textContent = isReject ? "반려하기" : isAssign ? "배정 변경" : "승인하기";
    adminDialog.showModal();
}

document.querySelector("#admin-dialog-cancel").addEventListener("click", () => adminDialog.close());
document.querySelector("#admin-action-form").addEventListener("submit", event => {
    event.preventDefault();
    if (!adminSelectedUser) return;
    if (adminAction === "REJECT") {
        adminSelectedUser.status = "REJECTED";
    } else if (adminAction === "ASSIGN") {
        adminSelectedUser.ward = adminWardSelect.value;
    } else {
        adminSelectedUser.status = "APPROVED";
    }
    const message = adminAction === "REJECT" ? "가입 신청을 반려했습니다." : adminAction === "ASSIGN" ? "담당 병동을 변경했습니다." : "가입 신청을 승인했습니다.";
    adminDialog.close();
    renderAdminUsers();
    adminTabs.find(tab => tab.dataset.status === adminActiveStatus).focus();
    showAdminFeedback(message);
    adminSelectedUser = null;
});

document.querySelector("#admin-logout").addEventListener("click", () => {
    showAdminFeedback("현재는 화면 미리보기입니다. 로그아웃은 로그인 기능 연동 후 사용할 수 있습니다.");
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

renderAdminUsers();
updateAdminClock();
setInterval(updateAdminClock, 30000);
