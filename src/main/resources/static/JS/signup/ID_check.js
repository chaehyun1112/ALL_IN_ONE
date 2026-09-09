// PGH
(function () {
  const userIdInput =
    document.getElementById("userId");

  const checkUserIdButton =
    document.getElementById("checkUserIdBtn");

  const userIdMessage =
    document.getElementById("userIdMessage");

  const userIdChecked =
    document.getElementById("userIdChecked");

  const signupForm =
    document.querySelector(".signup-form");

  // 사용자 아이디를 수정하면 중복확인 상태 초기화
  userIdInput.addEventListener("input", () => {
    userIdChecked.value = "false";
    userIdMessage.textContent = "";
    userIdMessage.className = "username-message";
  });

  // 사용자 아이디 중복확인
  checkUserIdButton.addEventListener("click", async () => {
    const userId = userIdInput.value.trim();

    if (!userId) {
      userIdMessage.textContent =
        "아이디를 입력해 주세요.";

      userIdMessage.className =
        "username-message is-taken";

      userIdInput.focus();
      return;
    }

    checkUserIdButton.disabled = true;

    userIdMessage.textContent = "확인 중...";
    userIdMessage.className = "username-message";

    try {
      const response = await fetch(
        `/api/users/check-user-id?userId=${encodeURIComponent(userId)}`
      );

      if (!response.ok) {
        throw new Error("서버 요청에 실패했습니다.");
      }

      const result = await response.json();

      if (result.available) {
        userIdMessage.textContent =
          "사용 가능한 아이디입니다.";

        userIdMessage.className =
          "username-message is-available";

        userIdChecked.value = "true";
      } else {
        userIdMessage.textContent =
          "이미 사용 중인 아이디입니다.";

        userIdMessage.className =
          "username-message is-taken";

        userIdChecked.value = "false";
      }
    } catch (error) {
      userIdMessage.textContent =
        "중복확인 중 오류가 발생했습니다. 다시 시도해 주세요.";

      userIdMessage.className =
        "username-message is-taken";

      userIdChecked.value = "false";
    } finally {
      checkUserIdButton.disabled = false;
    }
  });

  // 중복확인을 하지 않은 경우 회원가입 제출 방지
  signupForm.addEventListener("submit", (event) => {
    if (userIdChecked.value !== "true") {
      event.preventDefault();

      userIdMessage.textContent =
        "아이디 중복확인을 먼저 진행해 주세요.";

      userIdMessage.className =
        "username-message is-taken";

      userIdInput.focus();
    }
  });
})();
