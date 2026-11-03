document.querySelectorAll('[data-operation-form]').forEach(form => {
    form.addEventListener('submit', event => {
        const button = form.querySelector('button[type="submit"]');
        const confirmation = form.querySelector('input[name="confirmed"][value="true"]');
        const result = form.querySelector('.operation-result');
        if (button?.dataset.requiresConfirmation === 'true' && confirmation && !confirmation.checked) {
            event.preventDefault();
            confirmation.focus();
            if (result) result.textContent = '실행 전에 확인 항목을 선택해 주세요.';
            return;
        }
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
        if (result) result.textContent = '실행 중입니다.';
    });
});
