"use strict";

document.addEventListener("DOMContentLoaded", () => {
  // [수정완료] 설정 화면에서 선택한 필터를 저장하고 즉시 화면에 반영합니다.
  const options = document.querySelectorAll(".filter-option");
  const selectedText = document.querySelector(".selected-filter strong");
  const labels = {
    all: "전체 병실",
    normal: "정상 병실",
    caution: "주의 병실",
    urgent: "긴급 병실"
  };

  options.forEach(option => {
    option.addEventListener("click", () => {
      const filter = option.dataset.filter;
      localStorage.setItem("careguard-filter", filter);
      options.forEach(item => item.classList.toggle("selected", item === option));
      if (selectedText) selectedText.textContent = labels[filter];
    });
  });
});
