"use strict";

const createContext = window.adminCreateContext;
const createDialog = document.querySelector("#admin-create-dialog");
const createForm = document.querySelector("#admin-create-form");
const createNameInput = document.querySelector("#admin-create-name");
const createUserIdInput = document.querySelector("#admin-create-user-id");
const createPhoneInput = document.querySelector("#admin-create-phone");
const createRoomWardSelect = document.querySelector("#admin-create-room-ward");
const createRoomWardError = document.querySelector("#admin-create-room-ward-error");
const createRoomSelect = document.querySelector("#admin-create-room");
const createRoomDropdown = document.querySelector("#admin-create-room-dropdown");
const createRoomTrigger = document.querySelector("#admin-create-room-trigger");
const createRoomOptions = document.querySelector("#admin-create-room-options");
const createRoomLabel = document.querySelector("#admin-create-room-label");
const createWardSelect = document.querySelector("#admin-create-ward");
const createWardDropdown = document.querySelector("#admin-create-ward-dropdown");
const createWardTrigger = document.querySelector("#admin-create-ward-trigger");
const createWardOptions = document.querySelector("#admin-create-ward-options");
const createWardLabel = document.querySelector("#admin-create-ward-label");
const createSubmitButton = createForm?.querySelector('button[type="submit"]');
const createCompleteDialog = document.querySelector("#admin-create-complete-dialog");
const createNameError = document.querySelector("#admin-create-name-error");
const createUserIdError = document.querySelector("#admin-create-user-id-error");
const createWardError = document.querySelector("#admin-create-ward-error");
const createPhoneError = document.querySelector("#admin-create-phone-error");
const createRoomError = document.querySelector("#admin-create-room-error");
const createAccountButton = document.querySelector("#admin-create-account");
const createCancelButton = document.querySelector("#admin-create-cancel");
const createCompleteCloseButton = document.querySelector("#admin-create-complete-close");

let createPending = false;
let wardRequestVersion = 0;
let availableWards = [];

function isCaregiverCreation() {
    return document.body.dataset.adminJobType === "CAREGIVER";
}

function renderCreateRoomDropdown() {
    createRoomOptions.replaceChildren();
    createRoomLabel.textContent = createRoomSelect.value
        ? `${createRoomSelect.value}호` : createRoomSelect.options[0].textContent;
    for (const option of createRoomSelect.options) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = option.textContent;
        button.setAttribute("aria-selected", String(option.value === createRoomSelect.value));
        button.addEventListener("click", () => {
            createRoomSelect.value = option.value;
            renderCreateRoomDropdown();
            closeCreateRoomOptions();
            createRoomTrigger.focus();
        });
        createRoomOptions.append(button);
    }
}

function closeCreateRoomOptions() {
    createRoomOptions.hidden = true;
    createRoomTrigger.setAttribute("aria-expanded", "false");
    createRoomOptions.style.removeProperty("max-height");
    createDialog.classList.remove("room-menu-open");
}

// 프런트 미리보기용 병실 번호이며 DB의 병동·위치 ID로 사용하지 않는다.
function refreshCreateRooms() {
    const wardNumber = Number(createRoomWardSelect.value);
    createRoomSelect.replaceChildren(new Option(wardNumber ? "담당 병실을 선택해 주세요" : "병동을 먼저 선택해 주세요", ""));
    if (wardNumber) {
        for (let offset = 1; offset <= 17; offset += 1) {
            const room = wardNumber * 100 + offset;
            createRoomSelect.append(new Option(room + "호", String(room)));
        }
    }
    createRoomSelect.disabled = !wardNumber;
    createRoomTrigger.disabled = !wardNumber;
    closeCreateRoomOptions();
    renderCreateRoomDropdown();
}
createRoomWardSelect.addEventListener("change", () => {
    createRoomWardError.textContent = "";
    createRoomError.textContent = "";
    refreshCreateRooms();
});
refreshCreateRooms();
createRoomTrigger.addEventListener("click", () => {
    if (!createRoomOptions.hidden) return closeCreateRoomOptions();
    renderCreateRoomDropdown();
    const availableBelow = window.innerHeight - createRoomTrigger.getBoundingClientRect().bottom - 12;
    createRoomOptions.style.maxHeight = `${Math.max(64, Math.min(240, Math.floor(availableBelow)))}px`;
    createDialog.classList.add("room-menu-open");
    createRoomOptions.hidden = false;
    createRoomTrigger.setAttribute("aria-expanded", "true");
});
document.addEventListener("click", event => {
    if (!createRoomDropdown.contains(event.target)) closeCreateRoomOptions();
});
document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !createRoomOptions.hidden) {
        event.stopPropagation();
        closeCreateRoomOptions();
        createRoomTrigger.focus();
    }
}, true);

function closeCreateWardOptions() {
    if (!createWardOptions || !createWardTrigger) return;
    createWardOptions.hidden = true;
    createWardTrigger.setAttribute("aria-expanded", "false");
    createWardDropdown?.classList.remove("open-up");
    createDialog?.classList.remove("ward-menu-overflow");
    createWardOptions.style.removeProperty("max-height");
}

function renderCreateWardDropdown() {
    if (!createWardSelect || !createWardOptions || !createWardTrigger || !createWardLabel) return;
    createWardOptions.replaceChildren();
    createWardLabel.textContent = createWardSelect.selectedOptions[0]?.textContent
        || "담당 병동을 선택해주세요";
    createWardTrigger.disabled = createWardSelect.disabled;
    for (const option of createWardSelect.options) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = option.textContent;
        button.setAttribute("aria-selected", String(option.value === createWardSelect.value));
        button.addEventListener("click", () => {
            createWardSelect.value = option.value;
            createWardSelect.dispatchEvent(new Event("change", { bubbles: true }));
            closeCreateWardOptions();
            createWardTrigger.focus();
        });
        createWardOptions.append(button);
    }
}

if (createWardTrigger && createWardOptions && createWardDropdown) {
    createWardTrigger.addEventListener("click", () => {
        if (createWardTrigger.disabled) return;
        const opening = createWardOptions.hidden;
        if (!opening) {
            closeCreateWardOptions();
            return;
        }
        createWardOptions.hidden = false;
        createWardTrigger.setAttribute("aria-expanded", "true");
        const triggerBounds = createWardTrigger.getBoundingClientRect();
        const menuHeight = createWardOptions.getBoundingClientRect().height;
        const spaceBelowViewport = window.innerHeight - triggerBounds.bottom - 12;
        if (menuHeight <= spaceBelowViewport) {
            createDialog.classList.add("ward-menu-overflow");
        } else {
            const dialogBounds = createDialog.getBoundingClientRect();
            const spaceAbove = triggerBounds.top - dialogBounds.top - 12;
            createWardDropdown.classList.add("open-up");
            createWardOptions.style.maxHeight = `${Math.max(96, Math.floor(spaceAbove))}px`;
        }
    });
    createWardSelect.addEventListener("change", renderCreateWardDropdown);
    document.addEventListener("click", event => {
        if (!createWardDropdown.contains(event.target)) closeCreateWardOptions();
    });
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !createWardOptions.hidden) {
            event.stopPropagation();
            closeCreateWardOptions();
            createWardTrigger.focus();
        }
    }, true);
    renderCreateWardDropdown();
}

if (createNameInput) createNameInput.maxLength = 20;
if (createUserIdInput) createUserIdInput.maxLength = 20;
if (createPhoneInput) createPhoneInput.maxLength = 13;

function clearCreateErrors() {
    if (createNameError) createNameError.textContent = "";
    if (createUserIdError) createUserIdError.textContent = "";
    if (createWardError) createWardError.textContent = "";
    if (createPhoneError) createPhoneError.textContent = "";
    if (createRoomError) createRoomError.textContent = "";
    createRoomWardError.textContent = "";
}

function showCreateFieldErrors(errorBody) {
    let displayed = false;
    if (!errorBody || typeof errorBody !== "object" || Array.isArray(errorBody)) return false;

    if (typeof errorBody.userName === "string" && createNameError) {
        createNameError.textContent = errorBody.userName;
        displayed = true;
    }
    if (typeof errorBody.userId === "string" && createUserIdError) {
        createUserIdError.textContent = errorBody.userId;
        displayed = true;
    }
    if (typeof errorBody.wardId === "string" && createWardError) {
        createWardError.textContent = errorBody.wardId;
        displayed = true;
    }
    if (typeof errorBody.phoneNumber === "string" && createPhoneError) {
        createPhoneError.textContent = errorBody.phoneNumber;
        displayed = true;
    }
    if (typeof errorBody.roomNumber === "string" && createRoomError) {
        createRoomError.textContent = errorBody.roomNumber;
        displayed = true;
    }
    return displayed;
}

function showCreateError(message) {
    if (typeof showAdminFeedback === "function") {
        showAdminFeedback(message);
    } else if (createUserIdError) {
        createUserIdError.textContent = message;
    }
}

async function requestCreateApi(url, options = {}) {
    const method = (options.method ?? "GET").toUpperCase();
    const headers = new Headers(options.headers ?? {});
    headers.set("Accept", "application/json");

    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        if (!csrfToken || !csrfHeader) {
            throw new Error("관리자 로그인 정보가 없습니다. 다시 로그인해 주세요.");
        }
        headers.set(csrfHeader, csrfToken);
    }

    const response = await fetch(url, {
        ...options,
        method,
        headers,
        credentials: "same-origin",
        cache: "no-store"
    });

    if (response.status === 401 || response.redirected) {
        window.location.href = "/login";
        throw new Error("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    }

    let responseBody = null;
    const responseText = await response.text();

    if (responseText) {
        try {
            responseBody = JSON.parse(responseText);
        } catch {
            responseBody = null;
        }
    }

    if (!response.ok) {
        const error = new Error(
            responseBody?.message ?? `요청 처리에 실패했습니다. (${response.status})`
        );
        error.status = response.status;
        error.body = responseBody;
        throw error;
    }

    return responseBody;
}

function renderCreateWardOptions(wards) {
    if (!createWardSelect) return;

    createWardSelect.replaceChildren(new Option("담당 병동을 선택해주세요", ""));

    for (const ward of wards) {
        createWardSelect.append(new Option(ward.wardName, String(ward.wardId)));
    }
    renderCreateWardDropdown();
}

createAccountButton?.addEventListener("click", async () => {
    if (createPending || createDialog?.open) return;

    createForm?.reset();
    refreshCreateRooms();
    closeCreateWardOptions();
    closeCreateRoomOptions();
    renderCreateRoomDropdown();
    clearCreateErrors();
    availableWards = [];
    const caregiver = isCaregiverCreation();
    document.querySelectorAll(".admin-create-staff-field").forEach(field => { field.hidden = caregiver; });
    document.querySelectorAll(".admin-create-caregiver-field").forEach(field => { field.hidden = !caregiver; });

    if (caregiver) {
        if (createSubmitButton) createSubmitButton.disabled = false;
        createDialog?.showModal();
        createNameInput?.focus();
        return;
    }

    if (createWardSelect) {
        createWardSelect.disabled = true;
        createWardSelect.replaceChildren(new Option("병동 목록을 불러오는 중입니다.", ""));
        renderCreateWardDropdown();
    }
    if (createSubmitButton) createSubmitButton.disabled = true;

    createDialog?.showModal();
    const currentVersion = ++wardRequestVersion;

    try {
        const wards = await requestCreateApi("/api/admin/wards");

        if (currentVersion !== wardRequestVersion || !createDialog?.open) return;
        if (!Array.isArray(wards)) throw new Error("병동 목록 응답이 올바르지 않습니다.");

        availableWards = wards;
        renderCreateWardOptions(availableWards);

        if (availableWards.length === 0) {
            showCreateError("등록된 병동이 없습니다. 병동 등록 후 계정을 생성해 주세요.");
            return;
        }

        if (createWardSelect) createWardSelect.disabled = false;
        renderCreateWardDropdown();
        if (createSubmitButton) createSubmitButton.disabled = false;
    } catch (error) {
        if (currentVersion !== wardRequestVersion || !createDialog?.open) return;

        renderCreateWardOptions([]);
        showCreateError(error.message ?? "병동 목록을 불러오지 못했습니다.");
    }
});

createCancelButton?.addEventListener("click", () => {
    if (!createPending) createDialog?.close();
});

createDialog?.addEventListener("cancel", event => {
    if (createPending) event.preventDefault();
});

createDialog?.addEventListener("close", () => {
    wardRequestVersion += 1;
    closeCreateWardOptions();
    closeCreateRoomOptions();
});

createCompleteCloseButton?.addEventListener("click", () => {
    createCompleteDialog?.close();
});

createCompleteDialog?.addEventListener("close", () => {
    const passwordElement = document.querySelector("#complete-account-password");
    const copyButton = document.querySelector("#copy-account-password");

    if (passwordElement) passwordElement.textContent = "";
    if (copyButton) copyButton.textContent = "복사하기";
});

document.querySelector("#copy-account-password")?.addEventListener("click", async event => {
    const copyButton = event.currentTarget;
    const temporaryPassword = document.querySelector(
        "#complete-account-password"
    )?.textContent;

    if (!temporaryPassword) {
        showCreateError("복사할 임시 비밀번호가 없습니다.");
        return;
    }

    try {
        await navigator.clipboard.writeText(temporaryPassword);
        copyButton.textContent = "복사됨";

        setTimeout(() => {
            copyButton.textContent = "복사하기";
        }, 1600);
    } catch {
        showCreateError(
            "자동 복사가 불가능합니다. 표시된 비밀번호를 직접 복사해 주세요."
        );
    }
});

createForm?.addEventListener("submit", async event => {
    event.preventDefault();
    if (createPending) return;

    clearCreateErrors();

    const userName = createNameInput?.value.trim() ?? "";
    const userId = createUserIdInput?.value.trim() ?? "";
    const caregiver = isCaregiverCreation();
    const phoneNumber = createPhoneInput?.value.replace(/\D/g, "") ?? "";
    const roomNumber = Number(createRoomSelect?.value ?? 0);
    const wardValue = createWardSelect?.value ?? "";
    const validUserId = /^[A-Za-z0-9._-]{1,20}$/.test(userId);
    const validUserName = userName.length >= 1 && userName.length <= 20;
    const selectedWard = availableWards.find(
        ward => String(ward.wardId) === String(wardValue)
    );

    if (!validUserName && createNameError) {
        createNameError.textContent = "이름을 20자 이내로 입력해 주세요.";
    }
    if (!caregiver && !validUserId && createUserIdError) {
        createUserIdError.textContent =
            "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈으로 20자 이내로 입력해 주세요.";
    }
    if (!caregiver && !selectedWard && createWardError) {
        createWardError.textContent = "담당 병동을 선택해 주세요.";
    }
    if (caregiver && !/^01\d{8,9}$/.test(phoneNumber)) {
        createPhoneError.textContent = "휴대전화 번호를 확인해 주세요.";
    }
    const selectedWardNumber = Number(createRoomWardSelect.value);
    const validRoom = selectedWardNumber >= 1 && selectedWardNumber <= 3
        && roomNumber > selectedWardNumber * 100 && roomNumber <= selectedWardNumber * 100 + 17;
    if (caregiver && !selectedWardNumber) createRoomWardError.textContent = "담당 병동을 선택해 주세요.";
    if (caregiver && !validRoom) createRoomError.textContent = "선택한 병동의 담당 병실을 선택해 주세요.";
    if (!validUserName || (caregiver
        ? !/^01\d{8,9}$/.test(phoneNumber) || !validRoom
        : !validUserId || !selectedWard)) return;

    if (caregiver && selectedWardNumber !== 3) {
        createRoomError.textContent = "1·2병동 계정 생성은 서버 연동 후 사용할 수 있습니다.";
        return;
    }

    createPending = true;
    const controls = [...createForm.querySelectorAll("input, select, button")];
    controls.forEach(control => { control.disabled = true; });

    try {
        const created = await requestCreateApi(caregiver ? "/api/admin/caregivers" : "/api/admin/users", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(caregiver
                ? { userName, phoneNumber, roomNumber }
                : { userId, userName, wardId: Number(selectedWard.wardId) })
        });

        if (
            !created ||
            (caregiver ? !created.userId : created.userId !== userId) ||
            typeof created.temporaryPassword !== "string" ||
            !created.temporaryPassword
        ) {
            throw new Error("계정 생성 결과를 확인할 수 없습니다.");
        }

        const completeName = document.querySelector("#complete-account-name");
        const completePhone = document.querySelector("#complete-account-phone");
        const completeUserId = document.querySelector("#complete-account-user-id");
        const completeWard = document.querySelector("#complete-account-ward");
        const completePassword = document.querySelector("#complete-account-password");

        if (completeName) completeName.textContent = created.userName;
        if (completePhone) completePhone.textContent = caregiver
            ? `${phoneNumber.slice(0, 3)}-${phoneNumber.slice(3, phoneNumber.length - 4)}-${phoneNumber.slice(-4)}`
            : "";
        if (completeUserId) completeUserId.textContent = caregiver ? "" : created.userId;
        if (completeWard) completeWard.textContent = caregiver ? `${roomNumber}호` : selectedWard.wardName;
        if (completePassword) completePassword.textContent = caregiver ? "" : created.temporaryPassword;
        document.querySelector("#complete-account-phone-row").hidden = !caregiver;
        document.querySelector("#complete-account-user-id-row").hidden = caregiver;
        document.querySelector("#complete-account-password-row").hidden = caregiver;
        createCompleteDialog.classList.toggle("is-caregiver", caregiver);

        if (createContext) {
            const alreadyExists = createContext.users.some(
                user => user.userId === created.userId
            );

            if (!alreadyExists) {
                createContext.users.unshift({
                    userId: created.userId,
                    name: created.userName,
                    status: created.accountStatus ?? "APPROVED",
                    wardId: created.wardId,
                    ward: caregiver ? "3병동" : selectedWard.wardName,
                    phoneNumber: caregiver ? phoneNumber : null,
                    roomNumber: caregiver ? String(roomNumber) : null
                });
            }

            createContext.renderUsers();
        }

        createDialog?.close();
        createForm.reset();
        createCompleteDialog?.showModal();
    } catch (error) {
        // [2026-09-18] 중복 아이디 안내를 배경 화면 대신 계정 생성 창의 아이디 입력란 아래에 표시한다.
        const fieldErrorDisplayed = showCreateFieldErrors(
            error.status === 409
                ? caregiver
                    ? {phoneNumber: error.message || "이미 등록된 전화번호입니다."}
                    : {userId: error.message || "이미 사용 중인 직원 아이디입니다."}
                : error.body
        );

        if (!fieldErrorDisplayed) {
            showCreateError(error.message ?? "계정 생성에 실패했습니다.");
        }
    } finally {
        createPending = false;
        controls.forEach(control => { control.disabled = false; });
    }
});
