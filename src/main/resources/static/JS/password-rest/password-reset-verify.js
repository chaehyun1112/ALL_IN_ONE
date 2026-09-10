// PGH
document.addEventListener("DOMContentLoaded", () => {
  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

  const employeeId = document.getElementById("employeeId");
  const employeeName = document.getElementById("employeeName");
  const email = document.getElementById("email");
  const sendCodeBtn = document.getElementById("sendCodeBtn");
  const identityMessage = document.getElementById("identityMessage");

  const verifyCodeField = document.getElementById("verifyCodeField");
  const verifyCode = document.getElementById("verifyCode");
  const checkCodeBtn = document.getElementById("checkCodeBtn");
  const codeMessage = document.getElementById("codeMessage");
  const codeTimer = document.getElementById("codeTimer");

  const identifySection = document.getElementById("identifySection");
  const newPasswordSection = document.getElementById("newPasswordSection");

  // 이메일 인증 없이 새 비밀번호 단계만 서버 렌더링으로 곧바로 보여준 경우(예: 검증 실패 후
  // 재렌더링)에는 발송/확인 버튼 자체가 없으므로 이 스크립트가 더 할 일이 없다.
  if (!sendCodeBtn) {
    return;
  }

  let timerInterval = null;

  function formatTime(sec) {
    const m = String(Math.floor(sec / 60)).padStart(2, "0");
    const s = String(sec % 60).padStart(2, "0");
    return `${m}:${s}`;
  }

  function startTimer(seconds) {
    clearInterval(timerInterval);
    let remaining = seconds;
    codeTimer.textContent = formatTime(remaining);
    timerInterval = setInterval(() => {
      remaining--;
      if (remaining <= 0) {
        clearInterval(timerInterval);
        codeTimer.textContent = "";
      } else {
        codeTimer.textContent = formatTime(remaining);
      }
    }, 1000);
  }

  function postJson(url, body) {
    return fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [csrfHeader]: csrfToken
      },
      body: JSON.stringify(body)
    }).then((res) => res.json());
  }

  sendCodeBtn.addEventListener("click", () => {
    const id = employeeId.value.trim();
    const name = employeeName.value.trim();
    const emailValue = email.value.trim();

    if (!id || !name || !emailValue) {
      identityMessage.textContent = "아이디, 이름, 이메일을 입력해 주세요.";
      identityMessage.className = "id-check-message is-error";
      return;
    }

    sendCodeBtn.disabled = true;
    postJson("/password/reset/verify-identity", { employeeId: id, employeeName: name, email: emailValue })
      .then((data) => {
        if (data.status === "SUCCESS") {
          identityMessage.textContent = data.message;
          identityMessage.className = "id-check-message is-success";
          verifyCodeField.hidden = false;
          checkCodeBtn.disabled = false;
          codeMessage.textContent = "";
          verifyCode.value = "";
          startTimer(300);
        } else {
          identityMessage.textContent = data.message;
          identityMessage.className = "id-check-message is-error";
        }
      })
      .catch(() => {
        identityMessage.textContent = "요청 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        identityMessage.className = "id-check-message is-error";
      })
      .finally(() => {
        sendCodeBtn.disabled = false;
      });
  });

  checkCodeBtn.addEventListener("click", () => {
    const code = verifyCode.value.trim();
    if (!code) {
      codeMessage.textContent = "인증코드를 입력해 주세요.";
      codeMessage.className = "id-check-message is-error";
      return;
    }

    checkCodeBtn.disabled = true;
    postJson("/password/reset/verify-code", { code: code })
      .then((data) => {
        if (data.status === "SUCCESS") {
          clearInterval(timerInterval);
          identifySection.hidden = true;
          newPasswordSection.hidden = false;
          document.getElementById("newPassword").focus();
        } else {
          codeMessage.textContent = data.message;
          codeMessage.className = "id-check-message is-error";
          checkCodeBtn.disabled = false;
        }
      })
      .catch(() => {
        codeMessage.textContent = "요청 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        codeMessage.className = "id-check-message is-error";
        checkCodeBtn.disabled = false;
      });
  });
});
