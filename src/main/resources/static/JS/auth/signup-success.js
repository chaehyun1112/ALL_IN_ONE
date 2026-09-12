document.addEventListener("DOMContentLoaded", function () {
  const signupSuccessMessage =
    document.getElementById("signupSuccessMessage");

  if (signupSuccessMessage === null) {
    return;
  }

  const message = signupSuccessMessage.textContent.trim();

  if (message !== "") {
    window.alert(message);
  }
});