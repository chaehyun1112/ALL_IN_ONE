"use strict";

const createContext = window.adminCreateContext;
const createDialog = document.querySelector("#admin-create-dialog");
const createForm = document.querySelector("#admin-create-form");
const createNameInput = document.querySelector("#admin-create-name");
const createUserIdInput = document.querySelector("#admin-create-user-id");
const createWardSelect = document.querySelector("#admin-create-ward");
const createSubmitButton = createForm?.querySelector('button[type="submit"]');
const createCompleteDialog = document.querySelector("#admin-create-complete-dialog");
const createNameError = document.querySelector("#admin-create-name-error");
const createUserIdError = document.querySelector("#admin-create-user-id-error");
const createWardError = document.querySelector("#admin-create-ward-error");
const createAccountButton = document.querySelector("#admin-create-account");
const createCancelButton = document.querySelector("#admin-create-cancel");
const createCompleteCloseButton = document.querySelector("#admin-create-complete-close");

let createPending = false;
let wardRequestVersion = 0;
let availableWards = [];

if (createNameInput) createNameInput.maxLength = 20;
if (createUserIdInput) createUserIdInput.maxLength = 20;

function clearCreateErrors() {
    if (createNameError) createNameError.textContent = "";
    if (createUserIdError) createUserIdError.textContent = "";
    if (createWardError) createWardError.textContent = "";
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
}

createAccountButton?.addEventListener("click", async () => {
    if (createPending || createDialog?.open) return;

    createForm?.reset();
    clearCreateErrors();
    availableWards = [];

    if (createWardSelect) {
        createWardSelect.disabled = true;
        createWardSelect.replaceChildren(new Option("병동 목록을 불러오는 중입니다.", ""));
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
    const wardValue = createWardSelect?.value ?? "";
    const validUserId = /^[A-Za-z0-9._-]{1,20}$/.test(userId);
    const validUserName = userName.length >= 1 && userName.length <= 20;
    const selectedWard = availableWards.find(
        ward => String(ward.wardId) === String(wardValue)
    );

    if (!validUserName && createNameError) {
        createNameError.textContent = "이름을 20자 이내로 입력해 주세요.";
    }
    if (!validUserId && createUserIdError) {
        createUserIdError.textContent =
            "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈으로 20자 이내로 입력해 주세요.";
    }
    if (!selectedWard && createWardError) {
        createWardError.textContent = "담당 병동을 선택해 주세요.";
    }
    if (!validUserName || !validUserId || !selectedWard) return;

    createPending = true;
    const controls = [...createForm.querySelectorAll("input, select, button")];
    controls.forEach(control => { control.disabled = true; });

    try {
        const created = await requestCreateApi("/api/admin/users", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                userId,
                userName,
                wardId: Number(selectedWard.wardId)
            })
        });

        if (
            !created ||
            created.userId !== userId ||
            typeof created.temporaryPassword !== "string" ||
            !created.temporaryPassword
        ) {
            throw new Error("계정 생성 결과를 확인할 수 없습니다.");
        }

        const completeName = document.querySelector("#complete-account-name");
        const completeUserId = document.querySelector("#complete-account-user-id");
        const completeWard = document.querySelector("#complete-account-ward");
        const completePassword = document.querySelector("#complete-account-password");

        if (completeName) completeName.textContent = created.userName;
        if (completeUserId) completeUserId.textContent = created.userId;
        if (completeWard) completeWard.textContent = selectedWard.wardName;
        if (completePassword) completePassword.textContent = created.temporaryPassword;

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
                    ward: selectedWard.wardName
                });
            }

            createContext.renderUsers();
        }

        createDialog?.close();
        createForm.reset();
        createCompleteDialog?.showModal();
    } catch (error) {
        const fieldErrorDisplayed = showCreateFieldErrors(error.body);

        if (!fieldErrorDisplayed) {
            showCreateError(error.message ?? "계정 생성에 실패했습니다.");
        }
    } finally {
        createPending = false;
        controls.forEach(control => { control.disabled = false; });
    }
});
