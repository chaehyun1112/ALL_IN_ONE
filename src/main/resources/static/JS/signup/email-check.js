document.addEventListener("DOMContentLoaded", () => {
  const emailLocal = document.getElementById("emailLocal");
  const emailDomainSelect = document.getElementById("emailDomainSelect");
  const emailDomainCustom = document.getElementById("emailDomainCustom");
  const emailHidden = document.getElementById("email");

  const sendCodeBtn = document.getElementById("sendCodeBtn");
  const emailMessage = document.getElementById("emailMessage");

  const verifyCodeRow = document.getElementById("verifyCodeRow");
  const verifyCodeInput = document.getElementById("verifyCode");
  const checkCodeBtn = document.getElementById("checkCodeBtn");
  const codeTimer = document.getElementById("codeTimer");
  const emailVerified = document.getElementById("emailVerified");

  const requiredElements = [
    emailLocal,
    emailDomainSelect,
    emailDomainCustom,
    emailHidden,
    sendCodeBtn,
    emailMessage,
    verifyCodeRow,
    verifyCodeInput,
    checkCodeBtn,
    codeTimer,
    emailVerified
  ];

  if (requiredElements.some((element) => !element)) {
    console.error("이메일 인증 영역의 HTML id를 확인해 주세요.");
    return;
  }

  // 안내 메시지 표시
  function showMessage(text, isError = false) {
    emailMessage.textContent = text;
    emailMessage.className = text
      ? `username-message ${isError ? "is-taken" : "is-available"}`
      : "username-message";
  }

  // 이메일 아이디와 도메인 조합
  function buildEmail() {
    const local = emailLocal.value.trim();
    const domain =
      emailDomainSelect.value === "direct"
        ? emailDomainCustom.value.trim()
        : emailDomainSelect.value.trim();

    const email = local && domain ? `${local}@${domain}` : "";

    emailHidden.value = email;
    return email;
  }

  // 직접 입력 선택 여부에 따라 도메인 입력칸 표시
  function updateDomainInput() {
    const isDirect = emailDomainSelect.value === "direct";

    emailDomainCustom.style.display = isDirect ? "block" : "none";
    emailDomainCustom.required = isDirect;
  }

  // 이메일 변경 시 인증번호 입력 영역 초기화
  function resetCodeArea() {
    verifyCodeRow.style.display = "none";
    verifyCodeInput.value = "";
    verifyCodeInput.disabled = false;
    checkCodeBtn.disabled = true;

    emailVerified.value = "false";
    codeTimer.textContent = "";

    showMessage("");
    buildEmail();
  }

  emailDomainSelect.addEventListener("change", () => {
    updateDomainInput();
    resetCodeArea();

    if (emailDomainSelect.value === "direct") {
      emailDomainCustom.focus();
    }
  });

  emailLocal.addEventListener("input", resetCodeArea);
  emailDomainCustom.addEventListener("input", resetCodeArea);

  // 발송 버튼 클릭 → 인증번호 입력칸 표시
  sendCodeBtn.addEventListener("click", () => {
    const email = buildEmail();
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailPattern.test(email)) {
      showMessage(
        "이메일 아이디와 도메인을 올바르게 입력해 주세요.",
        true
      );
      return;
    }

    verifyCodeRow.style.display = "flex";
    verifyCodeInput.value = "";
    verifyCodeInput.disabled = false;
    checkCodeBtn.disabled = false;

    emailVerified.value = "false";
    codeTimer.textContent = "";

    showMessage(
      "인증번호 입력칸이 표시됐어요. 현재는 화면 확인용으로 메일이 발송되지 않아요."
    );

    verifyCodeInput.focus();
  });

  // 인증번호 입력은 숫자 6자리까지 허용
  verifyCodeInput.addEventListener("input", () => {
    verifyCodeInput.value = verifyCodeInput.value
      .replace(/[^0-9]/g, "")
      .slice(0, 6);
  });

// 화면 테스트용 인증번호 확인
checkCodeBtn.addEventListener("click", () => {
  const code = verifyCodeInput.value.trim();

  if (!/^[0-9]{6}$/.test(code)) {
    showMessage("인증번호 숫자 6자리를 입력해 주세요.", true);
    verifyCodeInput.focus();
    return;
  }

  if (code === "123456") {
    showMessage("이메일 인증이 완료됐어요. (화면 테스트)", false);
  } else {
    showMessage("인증번호가 일치하지 않아요. 다시 확인해 주세요.", true);
  }

  // 화면 테스트이므로 실제 인증 완료 상태로 저장하지 않음
  emailVerified.value = "false";
});

  // 페이지 초기 상태
  sendCodeBtn.disabled = false;
  emailLocal.disabled = false;
  emailDomainSelect.disabled = false;
  emailDomainCustom.disabled = false;

  updateDomainInput();
  resetCodeArea();
});