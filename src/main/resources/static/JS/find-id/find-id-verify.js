// PGH
document.addEventListener("DOMContentLoaded", () => {
  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

  const employeeName = document.getElementById("employeeName");
  const email = document.getElementById("email");
  const sendCodeBtn = document.getElementById("sendCodeBtn");
  const emailMessage = document.getElementById("emailMessage");

  const verifyCodeField = document.getElementById("verifyCodeField");
  const verifyCode = document.getElementById("verifyCode");
  const checkCodeBtn = document.getElementById("checkCodeBtn");
  const codeMessage = document.getElementById("codeMessage");
  const codeTimer = document.getElementById("codeTimer");

  const resultBox = document.getElementById("resultBox");
  const resultUserId = document.getElementById("resultUserId");

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
    const name = employeeName.value.trim();
    const emailValue = email.value.trim();

    if (!name || !emailValue) {
      emailMessage.textContent = "이름과 이메일을 입력해 주세요.";
      emailMessage.className = "id-check-message is-error";
      return;
    }

    sendCodeBtn.disabled = true;
    postJson("/id/find/send-code", { employeeName: name, email: emailValue })
      .then((data) => {
        if (data.status === "SUCCESS") {
          emailMessage.textContent = data.message;
          emailMessage.className = "id-check-message is-success";
          verifyCodeField.hidden = false;
          checkCodeBtn.disabled = false;
          codeMessage.textContent = "";
          verifyCode.value = "";
          startTimer(300);
        } else {
          emailMessage.textContent = data.message;
          emailMessage.className = "id-check-message is-error";
        }
      })
      .catch(() => {
        emailMessage.textContent = "요청 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        emailMessage.className = "id-check-message is-error";
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
    postJson("/id/find/verify-code", { code: code })
      .then((data) => {
        if (data.status === "SUCCESS") {
          clearInterval(timerInterval);
          codeTimer.textContent = "";
          codeMessage.textContent = "";
          resultUserId.textContent = data.userId;
          resultBox.hidden = false;
          verifyCode.disabled = true;
          checkCodeBtn.disabled = true;
          sendCodeBtn.disabled = true;
          employeeName.disabled = true;
          email.disabled = true;
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
