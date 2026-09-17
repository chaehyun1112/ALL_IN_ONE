"use strict";

(() => {
    const historyRows = document.querySelector("#admin-history-rows");
    const historyCards = [
        ...document.querySelectorAll(".history-filter-card")
    ];
    const historyEmpty = document.querySelector("#admin-history-empty");

    if (!historyRows) return;

    const actionLabels = {
        CREATE: "계정 생성",
        CHANGE_WARD: "병동 변경",
        RESET_PASSWORD: "비밀번호 초기화",
        DEACTIVATE: "계정 비활성화",
        ACTIVATE: "계정 재활성화",
        DELETE: "계정 삭제"
    };

    let requestVersion = 0;

    /**
     * 서버의 Instant 값을 한국 시간으로 표시한다.
     */
    function formatCreatedAt(createdAt) {
        const date = new Date(createdAt);

        if (Number.isNaN(date.getTime())) {
            return createdAt ?? "";
        }

        const parts = new Intl.DateTimeFormat("ko-KR", {
            timeZone: "Asia/Seoul",
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            hourCycle: "h23"
        }).formatToParts(date);

        const values = Object.fromEntries(
            parts.map(part => [part.type, part.value])
        );

        return `${values.year}-${values.month}-${values.day} `
                + `${values.hour}:${values.minute}`;
    }

    /**
     * 감사 코드에 해당하는 화면 표시 문구를 반환한다.
     */
    function getActionLabel(actionCode) {
        return actionLabels[actionCode] ?? actionCode ?? "알 수 없음";
    }

    /**
     * 일반 표 셀을 생성한다.
     */
    function createCell(value) {
        const cell = document.createElement("td");
        cell.textContent = value ?? "";
        return cell;
    }

    /**
     * 기존 Yejin 화면의 회원 관리 버튼을 생성한다.
     */
    function createManagementCell(history) {
        const cell = document.createElement("td");
        cell.className = "history-actions";

        const detailButton = document.createElement("button");
        detailButton.type = "button";
        detailButton.className = "history-edit";
        detailButton.textContent = "✎";
        detailButton.setAttribute(
            "aria-label",
            `${getActionLabel(history.actionCode)} 상세 및 직원 관리`
        );

        const deactivateButton = document.createElement("button");
        deactivateButton.type = "button";
        deactivateButton.className = "history-delete";
        deactivateButton.setAttribute(
            "aria-label",
            `${history.userId} 계정 비활성화`
        );
        deactivateButton.innerHTML = `
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14M10 11v6M14 11v6"/>
            </svg>
        `;

        /*
         * 삭제된 계정은 다시 관리할 수 없으므로
         * 상세 버튼만 표시한다.
         */
        cell.append(detailButton);

        if (history.actionCode !== "DELETE") {
            cell.append(deactivateButton);
        }

        return cell;
    }

    /**
     * 서버에서 조회한 감사 로그를 표에 출력한다.
     */
    function renderHistories(histories) {
        const fragment = document.createDocumentFragment();

        for (const history of histories) {
            const row = document.createElement("tr");
            const actionLabel = getActionLabel(history.actionCode);

            row.dataset.historyId = String(history.historyId ?? "");
            row.dataset.historyAction = actionLabel;
            row.dataset.actionCode = history.actionCode ?? "";
            row.dataset.userId = history.userId ?? "";
            row.dataset.userName = history.userName ?? "";
            row.dataset.wardName = history.wardName ?? "";

            row.append(
                createCell(formatCreatedAt(history.createdAt)),
                createCell(history.adminId),
                createCell(history.userId),
                createCell(history.wardName ?? "미확인"),
                createCell(actionLabel),
                createManagementCell(history)
            );

            fragment.append(row);
        }

        historyRows.replaceChildren(fragment);
        renderHistoryCounts(histories);

        if (typeof filterHistory === "function") {
            filterHistory(
                typeof adminHistoryFilter === "string"
                    ? adminHistoryFilter
                    : "전체"
            );
        }
    }

    /**
     * 상단 처리 내용별 카드 건수를 실제 감사 로그 기준으로 표시한다.
     */
    function renderHistoryCounts(histories) {
        for (const card of historyCards) {
            const filter = card.dataset.historyFilter;
            const countElement = card.querySelector("strong");

            if (!countElement) continue;

            const count = filter === "전체"
                ? histories.length
                : histories.filter(history =>
                    getActionLabel(history.actionCode) === filter
                ).length;

            countElement.textContent = `${count}건`;
        }
    }

    /**
     * 감사 로그 조회 API를 호출한다.
     */
    async function requestHistories() {
        const response = await fetch(
            "/api/admin/account-audit-logs?limit=50",
            {
                method: "GET",
                credentials: "same-origin",
                cache: "no-store",
                headers: {
                    "Accept": "application/json"
                }
            }
        );

        if (response.status === 401 || response.redirected) {
            window.location.href = "/login";
            throw new Error("로그인이 만료되었습니다.");
        }

        if (response.status === 403) {
            throw new Error("감사 로그 조회 권한이 없습니다.");
        }

        if (!response.ok) {
            let message =
                `감사 로그를 불러오지 못했습니다. (${response.status})`;

            try {
                const errorBody = await response.json();

                if (errorBody.message) {
                    message = errorBody.message;
                }
            } catch (error) {
                // JSON 형식이 아니면 기본 메시지를 사용한다.
            }

            throw new Error(message);
        }

        const histories = await response.json();

        if (!Array.isArray(histories)) {
            throw new Error("감사 로그 응답 형식이 올바르지 않습니다.");
        }

        return histories;
    }

    /**
     * 감사 로그를 다시 조회하여 화면을 갱신한다.
     */
    async function refreshAdminHistory() {
        const currentVersion = ++requestVersion;

        try {
            const histories = await requestHistories();

            if (currentVersion !== requestVersion) return;

            renderHistories(histories);
        } catch (error) {
            if (currentVersion !== requestVersion) return;

            console.error(error);
            historyRows.replaceChildren();
            renderHistoryCounts([]);

            if (historyEmpty) {
                historyEmpty.hidden = false;
                historyEmpty.textContent =
                    "관리 이력을 불러오지 못했습니다.";
            }

            if (typeof showAdminFeedback === "function") {
                showAdminFeedback(
                    error.message
                    || "관리 이력을 불러오지 못했습니다."
                );
            }
        }
    }

    /*
     * 다른 관리자 JS에서도 작업 성공 후 이력을 갱신할 수 있도록 공개한다.
     */
    window.refreshAdminHistory = refreshAdminHistory;

    /*
     * 계정 생성, 병동 변경, 비밀번호 초기화 팝업을 닫으면
     * 방금 저장된 감사 로그를 다시 조회한다.
     */
    [
        "#admin-action-dialog",
        "#admin-create-complete-dialog",
        "#admin-password-reset-dialog",
        "#admin-deactivate-dialog"
    ].forEach(selector => {
        document.querySelector(selector)?.addEventListener(
            "close",
            () => refreshAdminHistory()
        );
    });

    // 화면을 처음 열었을 때 실제 감사 로그를 조회한다.
    refreshAdminHistory();
})();
