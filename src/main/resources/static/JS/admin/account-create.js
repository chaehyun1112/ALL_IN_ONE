/* [09.13]추가내용: 관리자 계정 생성 모달의 열기·닫기·입력 검증·완료 표시를 전용 파일로 분리했습니다. */
const createContext = window.adminCreateContext;
const createDialog = document.querySelector("#admin-create-dialog");
const createForm = document.querySelector("#admin-create-form");
const createWard = document.querySelector("#admin-create-ward");
const createCompleteDialog = document.querySelector("#admin-create-complete-dialog");

document.querySelector("#admin-create-account")?.addEventListener("click", () => {
    createForm?.reset();
    document.querySelectorAll(".admin-field-error").forEach(error => error.textContent = "");
    createDialog?.showModal();
});
document.querySelector("#admin-create-cancel")?.addEventListener("click", () => createDialog?.close());
document.querySelector("#admin-create-complete-close")?.addEventListener("click", () => createCompleteDialog?.close());

createForm?.addEventListener("submit", event => {
    event.preventDefault();
    const name = document.querySelector("#admin-create-name")?.value.trim();
    const userId = document.querySelector("#admin-create-user-id")?.value.trim();
    const ward = createWard?.value;
    const errors = {
        "admin-create-name-error": name ? "" : "이름을 입력해주세요.",
        "admin-create-user-id-error": userId ? "" : "아이디를 입력해주세요.",
        "admin-create-ward-error": ward ? "" : "담당 병동을 선택해주세요."
    };
    Object.entries(errors).forEach(([id, message]) => {
        document.querySelector(`#${id}`).textContent = message;
    });
    if (Object.values(errors).some(Boolean) || !createContext) return;

    const temporaryPassword = `Care${Math.random().toString(36).slice(2, 8)}!`;
    const selectedWard = createContext.wards.find(item => String(item.wardId) === String(ward));
    createContext.users.unshift({ userId, name, status: "APPROVED", wardId: selectedWard?.wardId ?? null, ward: selectedWard?.wardName ?? ward });
    if (createContext.historyRows) {
        const row = document.createElement("tr");
        row.dataset.historyAction = "계정 생성";
        row.innerHTML = `<td>${new Date().toISOString().slice(0, 16).replace("T", " ")}</td><td>${createContext.adminId}</td><td>${userId}</td><td>${selectedWard?.wardName ?? ward}</td><td>계정 생성</td><td class="history-actions"><button class="history-edit" aria-label="계정 생성 이력 수정">✎</button><button class="history-delete" aria-label="계정 생성 이력 삭제"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14M10 11v6M14 11v6"/></svg></button></td>`;
        createContext.historyRows.prepend(row);
    }
    createDialog.close();
    createContext.renderUsers();
    document.querySelector("#complete-account-name").textContent = name;
    document.querySelector("#complete-account-user-id").textContent = userId;
    document.querySelector("#complete-account-ward").textContent = selectedWard?.wardName ?? ward;
    document.querySelector("#complete-account-password").textContent = temporaryPassword;
    createCompleteDialog.showModal();
});
