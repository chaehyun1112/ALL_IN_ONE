"use strict";

(() => {
  // Live Server 미리보기에는 인증 서버 주소가 주입되지 않습니다.
  const sessionUrl = document.body.dataset.sessionUrl;
  if (!sessionUrl) return;

  const refreshInterval = 60_000;
  let timer;
  let pending = false;
  let active = true;
  let signedOut = false;

  async function refreshSession() {
    if (!active || pending || signedOut) return;
    clearTimeout(timer);
    pending = true;
    try {
      const response = await fetch(sessionUrl, {
        credentials: "same-origin",
        cache: "no-store",
        headers: { Accept: "application/json" },
        signal: AbortSignal.timeout(15_000)
      });
      // 다른 탭에서 로그아웃하거나 인증이 종료된 세션을 복원하지 않습니다.
      if (response.redirected || response.status === 401 || response.status === 403) {
        signedOut = true;
      } else if (response.ok) {
        const ward = await response.json();
        if (String(ward.wardId ?? "") !== document.body.dataset.wardId ||
            ward.wardName !== document.body.dataset.wardName) {
          window.location.reload();
        }
      }
    } catch {
      // 일시적인 연결 실패에는 화면을 이동하지 않고 다음 주기에 재시도합니다.
    } finally {
      pending = false;
      if (active && !signedOut) timer = setTimeout(refreshSession, refreshInterval);
    }
  }

  window.addEventListener("pagehide", () => {
    active = false;
    clearTimeout(timer);
  });
  window.addEventListener("pageshow", () => {
    active = true;
    refreshSession();
  });
  window.addEventListener("online", refreshSession);
  window.addEventListener("focus", refreshSession);
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") refreshSession();
  });
  refreshSession();
})();
