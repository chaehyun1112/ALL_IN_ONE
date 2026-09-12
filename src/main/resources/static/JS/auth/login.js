document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("userLoginForm");
  const userLoginKey = document.getElementById("userLoginKey");
  const userId = document.getElementById("userId");

  if (!form || !userLoginKey || !userId) {
    return;
  }

  const hospitalPrefix = userLoginKey.value;

  form.addEventListener("submit", () => {
    userLoginKey.value =
      hospitalPrefix + userId.value.trim();
  });
});