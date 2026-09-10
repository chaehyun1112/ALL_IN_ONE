document.addEventListener("DOMContentLoaded", () => {
  /*
   * 병실 상태 데이터
   * 백엔드 연결 후 서버에서 받은 데이터로 변경하면 됩니다.
   */
  const roomStatus = {
    305: "urgent",
    312: "caution"
  };

  const roomStatusText = {
    urgent: "낙상 감지",
    caution: "침대 이탈",
    normal: "안전 정상"
  };

  const upperRooms =
    document.getElementById("upper-rooms");

  const lowerRooms =
    document.getElementById("lower-rooms");

  const roomDetail =
    document.getElementById("room-detail");

  let selectedRoomNumber = null;

  // 필수 영역이 HTML에 없으면 이후 오류를 막고 종료합니다.
  if (!upperRooms || !lowerRooms || !roomDetail) {
    console.error("upper-rooms, lower-rooms, room-detail 요소를 확인하세요.");
    return;
  }

  // HTML에 임시 병실이 남아 있어도 JS 생성 병실과 중복되지 않게 비웁니다.
  upperRooms.replaceChildren();
  lowerRooms.replaceChildren();


  /*
   * 병실 버튼 생성
   */
  function createRoom(number) {
    const room = document.createElement("button");

    room.type = "button";
    room.className = "room";

    const status = roomStatus[number] || "normal";
    const statusText = roomStatusText[status];

    if (status !== "normal") {
      room.classList.add(status);
    }

    room.setAttribute(
      "aria-label",
      `${number}호 · ${statusText}`
    );

    room.setAttribute(
      "aria-pressed",
      "false"
    );

    room.innerHTML = `
      <strong>${number}호</strong>
      <small>${statusText}</small>
      <span
        class="door"
        aria-hidden="true"
      ></span>
    `;

    /*
     * 병실 클릭 시 실행
     */
    room.addEventListener("click", () => {
      selectedRoomNumber = number;

      // 모든 병실 선택 해제
      document
        .querySelectorAll(".room")
        .forEach((otherRoom) => {
          otherRoom.setAttribute(
            "aria-pressed",
            "false"
          );
        });

      // 현재 병실 선택
      room.setAttribute(
        "aria-pressed",
        "true"
      );

      // 알림 병실인지 확인
      const isAlertRoom =
        number === 305 || number === 312;

      const message = isAlertRoom
        ? "확인 필요 · 환자 상태를 확인해 주세요"
        : "현재 확인이 필요한 알림 없음";

      // 평면도 아래 문구 변경
      roomDetail.textContent =
        `${number}호 · ${statusText} | ${message}`;

      // 대응 등록 버튼 표시
      showResponseButton(number);
    });

    return room;
  }


  /*
   * 위쪽 병실 생성
   * 301호 ~ 310호
   */
  for (let number = 301; number <= 310; number++) {
    upperRooms.appendChild(
      createRoom(number)
    );
  }


  /*
   * 아래쪽 병실 배치
   */

  // 왼쪽 화장실
  lowerRooms.appendChild(
    createFacility("화장실", "WC")
  );

  // 311호 ~ 313호
  for (let number = 311; number <= 313; number++) {
    lowerRooms.appendChild(
      createRoom(number)
    );
  }

  // 중간 빈 공간
  const emptySpace =
    document.createElement("div");

  emptySpace.setAttribute(
    "aria-hidden",
    "true"
  );

  lowerRooms.appendChild(emptySpace);

  // 314호 ~ 317호
  for (let number = 314; number <= 317; number++) {
    lowerRooms.appendChild(
      createRoom(number)
    );
  }

  // 오른쪽 화장실
  lowerRooms.appendChild(
    createFacility("화장실", "WC")
  );


  /*
   * 화장실과 시설 영역 생성
   */
  function createFacility(label, symbol) {
    const facility =
      document.createElement("div");

    facility.className = "facility";

    facility.innerHTML = `
      <span
        class="symbol"
        aria-hidden="true"
      >
        ${symbol}
      </span>

      <span>${label}</span>
    `;

    return facility;
  }


  /*
   * 실시간 시계
   */
  function updateClock() {
    const clock =
      document.getElementById("clock");

    if (!clock) {
      return;
    }

    const now = new Date();

    clock.textContent =
      new Intl.DateTimeFormat("ko-KR", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false
      }).format(now);
  }

  updateClock();
  setInterval(updateClock, 1000);


  /*
   * 위치 확인 버튼
   */
  const locationButtons =
    document.querySelectorAll(
      ".locate-room"
    );

  locationButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const roomNumber =
        Number(button.dataset.locate);

      const targetRoom =
        [...document.querySelectorAll(".room")]
          .find((room) => {
            return room.querySelector("strong")
              .textContent === `${roomNumber}호`;
          });

      if (!targetRoom) {
        return;
      }

      selectedRoomNumber = roomNumber;

      // 병실 외곽선 제거
      document
        .querySelectorAll(".room")
        .forEach((room) => {
          room.setAttribute(
            "aria-pressed",
            "false"
          );
        });

      // 선택한 병실 외곽선 표시
      targetRoom.setAttribute(
        "aria-pressed",
        "true"
      );

      // 위치 확인 버튼 상태 변경
      locationButtons.forEach((item) => {
        item.setAttribute(
          "aria-pressed",
          String(item === button)
        );
      });

      // 아래 상세 문구 변경
      const status =
        roomNumber === 305
          ? "낙상 감지"
          : "침대 이탈";

      roomDetail.textContent =
        `${roomNumber}호 · ${status} | 확인 필요 · 환자 상태를 확인해 주세요`;

      // 병실 위치로 이동
      targetRoom.scrollIntoView({
        behavior: "smooth",
        block: "center",
        inline: "center"
      });

      targetRoom.focus({
        preventScroll: true
      });

      // 대응 등록 버튼 표시
      showResponseButton(roomNumber);
    });
  });


  /*
   * 대응 등록 버튼
   */
  const responseOpen =
    document.getElementById("response-open");

  function showResponseButton(number) {
    if (!responseOpen) {
      return;
    }

    const isAlertRoom =
      number === 305 || number === 312;

    if (!isAlertRoom) {
      responseOpen.style.visibility =
        "hidden";

      responseOpen.disabled = true;
      return;
    }

    responseOpen.textContent =
      `${number}호 대응 등록`;

    responseOpen.style.visibility =
      "visible";

    responseOpen.disabled = false;
  }


  /*
   * 상단 메뉴 열기·닫기
   */
  const menuToggle =
    document.getElementById("menu-toggle");

  const menuList =
    document.getElementById(
      "header-menu-list"
    );

  function closeMenu() {
    if (!menuToggle || !menuList) {
      return;
    }

    menuList.hidden = true;

    menuToggle.setAttribute(
      "aria-expanded",
      "false"
    );
  }

  if (menuToggle && menuList) {
    menuToggle.addEventListener("click", () => {
      const isClosed = menuList.hidden;

      menuList.hidden = !isClosed;

      menuToggle.setAttribute(
        "aria-expanded",
        String(isClosed)
      );
    });

    document.addEventListener("click", (event) => {
      if (!event.target.closest(".header-menu")) {
        closeMenu();
      }
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        closeMenu();
      }
    });
  }


  /*
   * 설정 창
   */
  const settingsOpen =
    document.getElementById("settings-open");

  const settingsDialog =
    document.getElementById("settings-dialog");

  if (settingsOpen && settingsDialog) {
    settingsOpen.addEventListener("click", () => {
      closeMenu();
      settingsDialog.showModal();
    });
  }


  /*
   * 대응 등록 창
   */
  const responseDialog =
    document.getElementById("response-dialog");

  const responseForm =
    document.getElementById("response-form");

  const responseCancel =
    document.getElementById("response-cancel");

  const responseEvent =
    document.getElementById("response-event");

  const responseTitle =
    document.getElementById("response-title");

  if (responseOpen && responseDialog) {
    responseOpen.addEventListener("click", () => {
      if (!selectedRoomNumber) {
        return;
      }

      const eventType =
        selectedRoomNumber === 305
          ? "낙상 감지"
          : "침대 이탈";

      responseTitle.textContent =
        `${eventType} 대응 등록`;

      responseEvent.textContent =
        `${selectedRoomNumber}호 · ${eventType}`;

      responseDialog.showModal();
    });
  }


  /*
   * 대응 등록 취소
   */
  if (responseCancel && responseDialog) {
    responseCancel.addEventListener("click", () => {
      responseDialog.close();
    });
  }


  /*
   * 대응 등록 완료
   */
  if (responseForm && responseDialog) {
    responseForm.addEventListener(
      "submit",
      (event) => {
        event.preventDefault();

        const status =
          new FormData(responseForm)
            .get("response-status");

        if (status === "complete") {
          const selectedRoom =
            [...document.querySelectorAll(".room")]
              .find((room) => {
                return room.querySelector("strong")
                  .textContent ===
                  `${selectedRoomNumber}호`;
              });

          if (selectedRoom) {
            selectedRoom.classList.remove(
              "urgent",
              "caution"
            );

            selectedRoom.querySelector(
              "small"
            ).textContent = "안전 정상";

            selectedRoom.setAttribute(
              "aria-label",
              `${selectedRoomNumber}호 · 안전 정상`
            );
          }

          responseOpen.style.visibility =
            "hidden";

          responseOpen.disabled = true;

          roomDetail.textContent =
            `${selectedRoomNumber}호 · 조치 완료`;
        }

        responseDialog.close();
      }
    );
  }
});

