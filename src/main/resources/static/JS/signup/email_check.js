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

  let timerInterval = null;
  let remainingSeconds = 0;

  // 도메인 선택 → "직접 입력"이면 커스텀 인풋 보여주기
  emailDomainSelect.addEventListener("change", () => {
    if (emailDomainSelect.value === "direct") {
      emailDomainCustom.style.display = "block";
      emailDomainCustom.value = "";
      emailDomainCustom.focus();
    } else {
      emailDomainCustom.style.display = "none";
    }
  });

  function getDomain() {
    return emailDomainSelect.value === "direct"
      ? emailDomainCustom.value.trim()
      : emailDomainSelect.value;
  }

  function buildEmail() {
    const local = emailLocal.value.trim();
    const domain = getDomain();
    const email = local && domain ? `${local}@${domain}` : "";
    emailHidden.value = email;
    return email;
  }

  function formatTime(sec) {
    const m = String(Math.floor(sec / 60)).padStart(2, "0");
    const s = String(sec % 60).padStart(2, "0");
    return `${m}:${s}`;
  }

  function startTimer(seconds) {
    clearInterval(timerInterval);
    remainingSeconds = seconds;
    codeTimer.textContent = formatTime(remainingSeconds);

    timerInterval = setInterval(() => {
      remainingSeconds--;
      if (remainingSeconds <= 0) {
        clearInterval(timerInterval);
        codeTimer.textContent = "";
        emailMessage.textContent = "인증 시간이 만료됐어요. 다시 발송해 주세요.";
        emailMessage.className = "username-message is-taken";
        checkCodeBtn.disabled = true;
      } else {
        codeTimer.textContent = formatTime(remainingSeconds);
      }
    }, 1000);
  }

  // 인증번호 발송
  sendCodeBtn.addEventListener("click", () => {
    const email = buildEmail();
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!email || !emailPattern.test(email)) {
      emailMessage.textContent = "이메일 아이디와 도메인을 올바르게 입력해 주세요.";
      emailMessage.className = "username-message is-taken";
      return;
    }

    sendCodeBtn.disabled = true;

    fetch("/email/send-code", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email })
    })
      .then((res) => {
        if (!res.ok) throw new Error("발송 실패");
        return res.json();
      })
      .then(() => {
        emailMessage.textContent = "인증번호를 발송했어요.";
        emailMessage.className = "username-message is-available";

        verifyCodeRow.style.display = "flex";
        checkCodeBtn.disabled = false;
        startTimer(180);
      })
      .catch(() => {
        emailMessage.textContent = "발송 중 오류가 발생했어요. 다시 시도해 주세요.";
        emailMessage.className = "username-message is-taken";
      })
      .finally(() => {
        sendCodeBtn.disabled = false;
      });
  });

  // 인증번호 확인
  checkCodeBtn.addEventListener("click", () => {
    const email = emailHidden.value;
    const code = verifyCodeInput.value.trim();

    if (!code) {
      emailMessage.textContent = "인증번호를 입력해 주세요.";
      emailMessage.className = "username-message is-taken";
      return;
    }

    fetch("/email/verify-code", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, code })
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.verified) {
          emailMessage.textContent = "이메일 인증이 완료됐어요.";
          emailMessage.className = "username-message is-available";
          emailVerified.value = "true";

          clearInterval(timerInterval);
          codeTimer.textContent = "";
          verifyCodeInput.disabled = true;
          checkCodeBtn.disabled = true;
          sendCodeBtn.disabled = true;
          emailLocal.disabled = true;
          emailDomainSelect.disabled = true;
          emailDomainCustom.disabled = true;
        } else {
          emailMessage.textContent = "인증번호가 일치하지 않아요.";
          emailMessage.className = "username-message is-taken";
          emailVerified.value = "false";
        }
      })
      .catch(() => {
        emailMessage.textContent = "확인 중 오류가 발생했어요.";
        emailMessage.className = "username-message is-taken";
      });
  });
});