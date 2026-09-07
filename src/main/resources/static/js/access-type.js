const roleCards = document.querySelectorAll('.role-card');
const selectionMessage = document.querySelector('.selection-message');

const roleLabels = {
    admin: '관리자',
    nurse: '간호사'
};

roleCards.forEach((card) => {
    card.addEventListener('click', () => {
        roleCards.forEach((item) => {
            const isSelected = item === card;
            item.classList.toggle('is-selected', isSelected);
            item.setAttribute('aria-pressed', String(isSelected));
        });

        selectionMessage.textContent = `${roleLabels[card.dataset.role]} 접속 유형을 선택했습니다.`;
    });
});
