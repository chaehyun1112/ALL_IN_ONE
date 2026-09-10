document.addEventListener("DOMContentLoaded", () => {
  const usernameInput = document.getElementById("username");
  const checkBtn = document.getElementById("checkUsernameBtn");
  const message = document.getElementById("usernameMessage");
  const checked = document.getElementById("usernameChecked");

  if (!usernameInput || !checkBtn || !message || !checked) {
    console.error("아이디 중복확인 HTML id를 확인해 주세요.");
    return;
  }

  let checking = false;

  function showMessage(text, success = false) {
    message.textContent = text;
    message.className =
      "username-message " + (success ? "is-available" : "is-taken");
  }

  // 아이디를 수정하면 이전 중복확인 결과 초기화
  usernameInput.addEventListener("input", () => {
    checked.value = "false";
    usernameInput.classList.remove("is-available", "is-taken");
    message.textContent = "";
    message.className = "username-message";
  });

  checkBtn.addEventListener("click", async () => {
    if (checking) return;

    const userId = usernameInput.value.trim();

    checked.value = "false";
    usernameInput.classList.remove("is-available", "is-taken");

    if (!userId) {
      showMessage("아이디를 입력해 주세요.");
      usernameInput.focus();
      return;
    }

    usernameInput.value = userId;
    checking = true;
    checkBtn.disabled = true;
    message.textContent = "아이디 중복 여부를 확인하고 있어요.";
    message.className = "username-message";

    try {
      const url = new URL(
        "/api/users/check-user-id",
        window.location.origin
      );
      url.searchParams.set("userId", userId);

      const response = await fetch(url, {
        headers: {
          Accept: "application/json"
        },
        cache: "no-store"
      });

      if (!response.ok || response.redirected) {
        throw new Error("중복확인 요청 실패");
      }

      const data = await response.json();

      // 요청 중 아이디가 바뀌었다면 이전 결과를 적용하지 않음
      if (usernameInput.value !== userId) {
        return;
      }

      if (typeof data.available !== "boolean") {
        throw new Error("중복확인 응답 형식 오류");
      }

      if (data.available) {
        checked.value = "true";
        usernameInput.classList.add("is-available");
        showMessage("사용 가능한 아이디예요.", true);
      } else {
        usernameInput.classList.add("is-taken");
        showMessage("이미 사용 중인 아이디예요.");
      }
    } catch (error) {
      if (usernameInput.value === userId) {
        showMessage("중복확인에 실패했어요. 다시 시도해 주세요.");
      }
      console.error("아이디 중복확인 오류:", error);
    } finally {
      checking = false;
      checkBtn.disabled = false;
    }
  });
});