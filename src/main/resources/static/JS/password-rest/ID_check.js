  const idInput = document.getElementById("user-id");
    const idButton = document.getElementById("id-check-button");
    const idMessage = document.getElementById("id-check-message");

    let activeRequest = null;

    function showIdMessage(text, state = "") {
      idMessage.textContent = text;
      idMessage.classList.toggle("is-success", state === "success");
      idMessage.classList.toggle("is-error", state === "error");
    }

    function setIdLoading(loading) {
      idButton.disabled = loading;
      idButton.textContent = loading
        ? "확인 중..."
        : "아이디 확인하기";
    }

    // 아이디를 수정하면 이전 확인 결과를 지웁니다.
    idInput.addEventListener("input", () => {
      activeRequest?.abort();
      activeRequest = null;

      setIdLoading(false);
      showIdMessage("");
      idInput.removeAttribute("aria-invalid");
    });

    idButton.addEventListener("click", async () => {
      const userId = idInput.value.trim();

      if (!userId) {
        showIdMessage("아이디를 입력해 주세요.", "error");
        idInput.setAttribute("aria-invalid", "true");
        idInput.focus();
        return;
      }

      activeRequest?.abort();
      const request = new AbortController();
      activeRequest = request;

      idInput.removeAttribute("aria-invalid");
      setIdLoading(true);
      showIdMessage("아이디를 확인하고 있습니다.");

      try {
        // API 주소와 요청 필드는 백엔드 규격에 맞게 변경하세요.
        const response = await fetch("/api/auth/check-id", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ userId }),
          signal: request.signal
        });

        if (!response.ok) {
          throw new Error("아이디 확인 요청 실패");
        }

        const data = await response.json();

        // 아이디 수정 후 도착한 이전 응답은 무시합니다.
        if (activeRequest !== request) return;

        if (typeof data.exists !== "boolean") {
          throw new Error("응답 형식 오류");
        }

        if (data.exists) {
          showIdMessage("✓ 아이디가 확인되었습니다.", "success");
          idInput.setAttribute("aria-invalid", "false");
        } else {
          showIdMessage("아이디를 다시 확인해 주세요.", "error");
          idInput.setAttribute("aria-invalid", "true");
        }
      } catch (error) {
        if (error.name === "AbortError") return;
        if (activeRequest !== request) return;

        showIdMessage(
          "확인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
          "error"
        );
      } finally {
        if (activeRequest === request) {
          activeRequest = null;
          setIdLoading(false);
        }
      }
    });