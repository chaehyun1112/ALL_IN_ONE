   const password = document.getElementById("newPassword");
    const confirmation = document.getElementById("passwordConfirm");

    const letterRule = document.getElementById("rule-letter");
    const numberRule = document.getElementById("rule-number");
    const lengthRule = document.getElementById("rule-length");
    const matchMessage = document.getElementById("password-match");

    function updateRule(element, label, passed) {
      element.classList.toggle("is-valid", passed);
      element.textContent = label;
    }

    function validatePasswords() {
      const value = password.value;
      const hasConfirmation = confirmation.value !== "";
      const matches = value === confirmation.value;

      updateRule(letterRule, "영문 포함", /[A-Za-z]/.test(value));
      updateRule(numberRule, "숫자 포함", /[0-9]/.test(value));
      updateRule(lengthRule, "8자 이상", value.length >= 8);

      confirmation.setCustomValidity(
        hasConfirmation && !matches
          ? "비밀번호가 일치하지 않습니다."
          : ""
      );

      matchMessage.classList.toggle(
        "is-valid",
        hasConfirmation && matches
      );

      matchMessage.classList.toggle(
        "is-invalid",
        hasConfirmation && !matches
      );

      confirmation.setAttribute(
        "aria-invalid",
        String(hasConfirmation && !matches)
      );

      if (!hasConfirmation) {
        matchMessage.textContent =
          "새 비밀번호와 일치하는지 확인해 주세요.";
      } else if (matches) {
        matchMessage.textContent = "비밀번호가 일치합니다.";
      } else {
        matchMessage.textContent = "비밀번호가 일치하지 않습니다.";
      }
    }

    password.addEventListener("input", validatePasswords);
    confirmation.addEventListener("input", validatePasswords);
    password.addEventListener("change", validatePasswords);
    confirmation.addEventListener("change", validatePasswords);

    // 각 입력란의 비밀번호 표시·숨김
    document.querySelectorAll(".password-toggle").forEach((button) => {
      const input = document.getElementById(
        button.getAttribute("aria-controls")
      );

      button.addEventListener("click", () => {
        const showPassword = input.type === "password";

        input.type = showPassword ? "text" : "password";
        button.setAttribute("aria-pressed", String(showPassword));
      });
    });

    validatePasswords();