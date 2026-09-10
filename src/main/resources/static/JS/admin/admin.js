"use strict";

let adminUsers = [];
let adminWards = [];
let adminActiveStatus = "PENDING";
let adminSearchQuery = "";
let adminSelectedUser = null;
let adminAction = "";
let adminToastTimer = null;

const adminRows = document.querySelector("#admin-user-rows");
const adminTabs = Array.from(document.querySelectorAll("[data-status]"));
const adminDialog = document.querySelector("#admin-action-dialog");
const adminDialogTitle = document.querySelector("#admin-dialog-title");
const adminDialogDescription = document.querySelector("#admin-dialog-description");
const adminDialogConfirm = document.querySelector("#admin-dialog-confirm");
const adminDialogCancel = document.querySelector("#admin-dialog-cancel");
const adminWardField = document.querySelector("#admin-ward-field");
const adminWardSelect = document.querySelector("#admin-ward-select");
const adminSearchForm = document.querySelector("#admin-search-form");
const adminSearchInput = document.querySelector("#admin-search-input");
const adminEmpty = document.querySelector("#admin-empty");
const adminFeedback = document.querySelector("#admin-feedback");
const adminTotalCount = document.querySelector("#admin-total-count");
const adminApprovedCount = document.querySelector("#admin-approved-count");
const adminPendingCount = document.querySelector("#admin-pending-count");
const adminList = document.querySelector("#admin-list");
const adminClock = document.querySelector("#admin-clock");

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content ?? "";
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content ?? "X-CSRF-TOKEN";

/**
 * 관리자 API 요청
 * GET 이외의 요청에는 CSRF 토큰을 포함한다.
 */
async function requestAdminApi(url, options = {}) {
    const method = (options.method ?? "GET").toUpperCase();
    const headers = new Headers(options.headers ?? {});

    headers.set("Accept", "application/json");

    if (!["GET", "HEAD", "OPTIONS"].includes(method) && csrfToken) {
        headers.set(csrfHeader, csrfToken);
    }

    const response = await fetch(url, {
        ...options,
        method,
        headers
    });

    if (response.status === 401) {
        window.location.href = "/login";
        throw new Error("로그인이 만료되었습니다.");
    }

    if (response.status === 403) {
        throw new Error("요청 권한이 없거나 보안 정보가 만료되었습니다.");
    }

    if (!response.ok) {
        let message = `요청 처리에 실패했습니다. (${response.status})`;

        try {
            const errorBody = await response.json();

            if (errorBody.message) {
                message = errorBody.message;
            }
        } catch (error) {
            // JSON 응답이 아니면 기본 오류 메시지를 사용한다.
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    const responseText = await response.text();
    return responseText ? JSON.parse(responseText) : null;
}

/**
 * 화면 아래쪽에 처리 결과를 표시한다.
 */
function showAdminFeedback(message) {
    clearTimeout(adminToastTimer);

    adminFeedback.textContent = message;
    adminFeedback.hidden = false;

    adminToastTimer = setTimeout(() => {
        adminFeedback.hidden = true;
    }, 4000);
}

/**
 * 1번 API의 사용자 목록과
 * 2번 API의 승인 완료 사용자 상세 정보를 합친다.
 */
function combineAdminUsers(users, approvedUsers) {
    const approvedUserMap = new Map(
        approvedUsers.map(user => [user.userId, user])
    );

    return users.map(user => {
        const approvedDetail = approvedUserMap.get(user.userId);

        return {
            userId: user.userId,
            name: approvedDetail?.userName ?? user.name ?? "",
            status: approvedDetail?.authStatus ?? user.status ?? "",
            wardId: approvedDetail?.wardId ?? null,
            ward: approvedDetail?.wardName ?? user.ward ?? "미배정"
        };
    });
}

/**
 * 사용자 목록과 병동 목록을 DB에서 불러온다.
 */
async function loadAdminData(silent = false) {
    try {
        const [users, approvedUsers, wards] = await Promise.all([
            requestAdminApi("/api/admin/users"),
            requestAdminApi("/api/admin/users/approved"),
            requestAdminApi("/api/admin/wards")
        ]);

        adminUsers = combineAdminUsers(users ?? [], approvedUsers ?? []);
        adminWards = wards ?? [];

        renderAdminWardOptions();
        renderAdminUsers();
    } catch (error) {
        console.error(error);

        adminUsers = [];
        adminWards = [];

        renderAdminWardOptions();
        renderAdminUsers();

        if (!silent) {
            showAdminFeedback(error.message || "관리자 정보를 불러오지 못했습니다.");
        }

        throw error;
    }
}

/**
 * DB에서 불러온 병동을 선택란에 추가한다.
 */
function renderAdminWardOptions() {
    adminWardSelect.replaceChildren();

    const emptyOption = document.createElement("option");
    emptyOption.value = "";
    emptyOption.textContent = "병동을 선택해주세요";
    adminWardSelect.append(emptyOption);

    for (const ward of adminWards) {
        const option = document.createElement("option");
        option.value = String(ward.wardId);
        option.textContent = ward.wardName;
        adminWardSelect.append(option);
    }
}

/**
 * 전체, 승인 완료, 승인 대기 인원을 계산한다.
 */
function renderAdminCounts() {
    const approvedCount = adminUsers.filter(
        user => user.status === "APPROVED"
    ).length;

    const pendingCount = adminUsers.filter(
        user => user.status === "PENDING"
    ).length;

    adminTotalCount.textContent = `${approvedCount + pendingCount}명`;
    adminApprovedCount.textContent = `${approvedCount}명`;
    adminPendingCount.textContent = `${pendingCount}명`;
}

/**
 * 선택한 승인 상태와 검색어에 맞는 목록을 출력한다.
 */
function renderAdminUsers() {
    renderAdminCounts();

    const visibleUsers = adminUsers.filter(user => {
        const sameStatus = user.status === adminActiveStatus;
        const searchableText = `${user.name} ${user.userId}`.toLowerCase();
        const matchesSearch = searchableText.includes(adminSearchQuery);

        return sameStatus && matchesSearch;
    });

    adminRows.replaceChildren();

    for (const user of visibleUsers) {
        const row = document.createElement("tr");

        for (const value of [user.name, user.userId, user.ward || "미배정"]) {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.append(cell);
        }

        const actionsCell = document.createElement("td");
        const actions = document.createElement("div");
        actions.className = "admin-row-actions";

        const userActions = user.status === "PENDING"
            ? [
                ["APPROVE", "✓ 승인", "admin-approve", ""],
                ["REJECT", "× 반려", "admin-reject", ""]
            ]
            : [
                ["ASSIGN", "병동 변경", "admin-approve", "building"],
                ["DEACTIVATE", "비활성화", "admin-reject", "trash"]
            ];

        for (const [action, label, className, icon] of userActions) {
            const button = document.createElement("button");

            button.type = "button";
            button.className = icon
                ? `${className} admin-icon-button`
                : className;

            if (icon === "building") {
                button.innerHTML = `
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M4 21h16M6 21V7l6-3 6 3v14M9 10h1M14 10h1M9 14h1M14 14h1M10 21v-3h4v3" />
                    </svg>`;
                button.title = label;
            } else if (icon === "trash") {
                button.innerHTML = `
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14M10 11v6M14 11v6" />
                    </svg>`;
                button.title = label;
            } else {
                button.textContent = label;
            }

            button.setAttribute(
                "aria-label",
                `${user.name} ${label.replace(/[✓×]/g, "").trim()}`
            );

            button.addEventListener("click", () => {
                openAdminAction(user, action);
            });

            actions.append(button);
        }

        actionsCell.append(actions);
        row.append(actionsCell);
        adminRows.append(row);
    }

    adminEmpty.hidden = visibleUsers.length > 0;
}

/**
 * 승인 상태 탭을 변경한다.
 */
function selectAdminTab(tab) {
    adminActiveStatus = tab.dataset.status;

    for (const item of adminTabs) {
        const selected = item === tab;

        item.setAttribute("aria-selected", String(selected));
        item.tabIndex = selected ? 0 : -1;
    }

    adminList.setAttribute("aria-labelledby", tab.id);
    renderAdminUsers();
}

/**
 * 승인, 반려, 병동 변경, 비활성화 팝업을 연다.
 */
function openAdminAction(user, action) {
    adminSelectedUser = user;
    adminAction = action;

    adminWardField.hidden = true;
    adminWardSelect.disabled = true;
    adminWardSelect.required = false;
    adminWardSelect.value = "";

    if (action === "APPROVE") {
        adminDialogTitle.textContent = "가입 신청 승인";
        adminDialogDescription.textContent =
            `${user.name}님의 가입 신청을 승인하시겠습니까?`;
        adminDialogConfirm.textContent = "승인하기";
    }

    if (action === "REJECT") {
        adminDialogTitle.textContent = "가입 신청 반려";
        adminDialogDescription.textContent =
            `${user.name}님의 가입 신청을 반려하시겠습니까?`;
        adminDialogConfirm.textContent = "반려하기";
    }

    if (action === "ASSIGN") {
        adminDialogTitle.textContent = "담당 병동 변경";
        adminDialogDescription.textContent =
            `${user.name} (${user.userId})님의 담당 병동을 변경합니다.`;
        adminDialogConfirm.textContent = "변경하기";

        adminWardField.hidden = false;
        adminWardSelect.disabled = false;
        adminWardSelect.required = true;

        const currentWard = adminWards.find(ward =>
            String(ward.wardId) === String(user.wardId)
            || ward.wardName === user.ward
        );

        if (currentWard) {
            adminWardSelect.value = String(currentWard.wardId);
        }
    }

    if (action === "DEACTIVATE") {
        adminDialogTitle.textContent = "계정 비활성화";
        adminDialogDescription.textContent =
            `${user.name} 계정을 비활성화하시겠습니까? `
            + "비활성화하면 해당 계정으로 로그인할 수 없습니다. "
            + "기존 활동 및 업무 처리 기록은 유지됩니다.";
        adminDialogConfirm.textContent = "비활성화";
    }

    adminDialog.showModal();
}

/**
 * 선택한 관리자 작업을 서버에 요청한다.
 */
async function submitAdminAction() {
    if (!adminSelectedUser) {
        return;
    }

    const selectedUser = adminSelectedUser;
    const selectedAction = adminAction;

    let url = "";
    let method = "PATCH";
    let body;
    let successMessage = "";

    if (selectedAction === "APPROVE") {
        url = `/api/admin/users/${encodeURIComponent(selectedUser.userId)}/approve`;
        successMessage = "가입 신청을 승인했습니다.";
    }

    if (selectedAction === "REJECT") {
        url = `/api/admin/users/${encodeURIComponent(selectedUser.userId)}/reject`;
        method = "DELETE";
        successMessage = "가입 신청을 반려했습니다.";
    }

    if (selectedAction === "ASSIGN") {
        if (!adminWardSelect.value) {
            showAdminFeedback("변경할 병동을 선택해주세요.");
            adminWardSelect.focus();
            return;
        }

        url = `/api/admin/users/${encodeURIComponent(selectedUser.userId)}/ward`;
        body = JSON.stringify({
            wardId: Number(adminWardSelect.value)
        });
        successMessage = "담당 병동을 변경했습니다.";
    }

    if (selectedAction === "DEACTIVATE") {
        url = `/api/admin/users/${encodeURIComponent(selectedUser.userId)}/deactivate`;
        successMessage = "계정을 비활성화했습니다.";
    }

    if (!url) {
        showAdminFeedback("처리할 작업을 확인할 수 없습니다.");
        return;
    }

    const headers = {};

    if (body) {
        headers["Content-Type"] = "application/json";
    }

    adminDialogConfirm.disabled = true;

    try {
        await requestAdminApi(url, {
            method,
            headers,
            body
        });

        adminDialog.close();
        if (selectedAction === "DEACTIVATE") {
            window.location.assign(document.querySelector("#admin-settings").href);
            return;
        }
        await loadAdminData(true);

        const currentTab = adminTabs.find(
            tab => tab.dataset.status === adminActiveStatus
        );

        currentTab?.focus();
        showAdminFeedback(successMessage);
    } catch (error) {
        console.error(error);
        showAdminFeedback(error.message || "처리 중 오류가 발생했습니다.");
    } finally {
        adminDialogConfirm.disabled = false;
        adminSelectedUser = null;
        adminAction = "";
    }
}

/* 탭 클릭 */
for (const tab of adminTabs) {
    tab.addEventListener("click", () => {
        selectAdminTab(tab);
    });

    tab.addEventListener("keydown", event => {
        const supportedKeys = ["ArrowLeft", "ArrowRight", "Home", "End"];

        if (!supportedKeys.includes(event.key)) {
            return;
        }

        event.preventDefault();

        const currentIndex = adminTabs.indexOf(tab);
        let nextIndex = currentIndex;

        if (event.key === "Home") {
            nextIndex = 0;
        }

        if (event.key === "End") {
            nextIndex = adminTabs.length - 1;
        }

        if (event.key === "ArrowLeft") {
            nextIndex = (currentIndex - 1 + adminTabs.length) % adminTabs.length;
        }

        if (event.key === "ArrowRight") {
            nextIndex = (currentIndex + 1) % adminTabs.length;
        }

        const nextTab = adminTabs[nextIndex];
        selectAdminTab(nextTab);
        nextTab.focus();
    });
}

/* 사용자 검색 */
adminSearchForm.addEventListener("submit", event => {
    event.preventDefault();

    adminSearchQuery = adminSearchInput.value.trim().toLowerCase();
    renderAdminUsers();
});

adminSearchInput.addEventListener("input", event => {
    if (!event.target.value) {
        adminSearchQuery = "";
        renderAdminUsers();
    }
});

/* 팝업 취소 */
adminDialogCancel.addEventListener("click", () => {
    adminDialog.close();
});

/* 팝업 확인 */
document.querySelector("#admin-action-form").addEventListener("submit", async event => {
    event.preventDefault();
    await submitAdminAction();
});

/* 팝업이 닫히면 병동 선택란 초기화 */
adminDialog.addEventListener("close", () => {
    adminWardField.hidden = true;
    adminWardSelect.disabled = true;
    adminWardSelect.required = false;
    adminWardSelect.value = "";
});

/**
 * 관리자 화면 현재 시각 표시
 */
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

/* 초기 실행 */
if (new URLSearchParams(window.location.search).get("status") === "APPROVED") {
    selectAdminTab(document.querySelector("#admin-approved-tab"));
}
updateAdminClock();
setInterval(updateAdminClock, 30000);

loadAdminData().catch(() => {
    // 오류 메시지는 loadAdminData에서 표시한다.
});
