document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("userLoginForm");
  const userLoginKey = document.getElementById("userLoginKey");
  const userId = document.getElementById("userId");
  const displayNameField = document.getElementById("displayNameField");
  const displayName = document.getElementById("displayName");
  const adminContact = document.getElementById("adminContact");

  if (!form || !userLoginKey || !userId) {
    return;
  }

  const hospitalPrefix = userLoginKey.value;
  // [09.13]추가내용: 정적 HTML로 로그인 화면을 열어도 관리자 유형이면 이름 입력을 숨긴다.
  if (new URLSearchParams(window.location.search).get("role")?.toUpperCase() === "ADMIN") {
    displayNameField?.setAttribute("hidden", "");
    displayName?.removeAttribute("required");
    adminContact?.setAttribute("hidden", "");
  }

  form.addEventListener("submit", () => {
    userLoginKey.value =
      hospitalPrefix + userId.value.trim();
  });
});
