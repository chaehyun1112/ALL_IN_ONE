(function () {
    const usernameInput = document.getElementById('username');
    const checkBtn = document.getElementById('checkUsernameBtn');
    const messageEl = document.getElementById('usernameMessage');
    const checkedFlag = document.getElementById('usernameChecked');
    const form = document.querySelector('.signup-form');

    // 아이디를 수정하면 다시 확인하도록 초기화
    usernameInput.addEventListener('input', () => {
      checkedFlag.value = 'false';
      messageEl.textContent = '';
      messageEl.className = 'username-message';
    });

    checkBtn.addEventListener('click', async () => {
      const username = usernameInput.value.trim();

      if (!username) {
        messageEl.textContent = '아이디를 입력해 주세요.';
        messageEl.className = 'username-message is-taken';
        return;
      }

      checkBtn.disabled = true;
      messageEl.textContent = '확인 중...';
      messageEl.className = 'username-message';

      try {
        const res = await fetch(
          `/api/users/check-username?username=${encodeURIComponent(username)}`
        );

        if (!res.ok) throw new Error('서버 오류');

        const data = await res.json(); // { available: true/false }

        if (data.available) {
          messageEl.textContent = '사용 가능한 아이디입니다.';
          messageEl.className = 'username-message is-available';
          checkedFlag.value = 'true';
        } else {
          messageEl.textContent = '이미 사용 중인 아이디입니다.';
          messageEl.className = 'username-message is-taken';
          checkedFlag.value = 'false';
        }
      } catch (err) {
        messageEl.textContent = '중복확인 중 오류가 발생했습니다. 다시 시도해 주세요.';
        messageEl.className = 'username-message is-taken';
        checkedFlag.value = 'false';
      } finally {
        checkBtn.disabled = false;
      }
    });

    // 제출 시 중복확인 안 했으면 막기
    form.addEventListener('submit', (e) => {
      if (checkedFlag.value !== 'true') {
        e.preventDefault();
        messageEl.textContent = '아이디 중복확인을 먼저 진행해 주세요.';
        messageEl.className = 'username-message is-taken';
        usernameInput.focus();
      }
    });
  })();