(() => {
    const form = document.getElementById('draft-approval');
    if (!form) return;
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const button = form.querySelector('button');
        const status = document.getElementById('approval-status');
        button.disabled = true;
        status.textContent = '반영 중…';
        try {
            const response = await fetch(form.dataset.url, {
                method: 'PUT',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/json',
                    [form.elements.csrfHeader.value]: form.elements.csrfToken.value
                },
                body: JSON.stringify({revision: form.elements.revision.value})
            });
            if (response.redirected || response.status === 401 || response.status === 403) {
                throw new Error('로그인 상태를 확인한 뒤 다시 검토하세요.');
            }
            if (response.status === 409) {
                throw new Error('초안 또는 원문이 변경되었습니다. 다시 검토하세요.');
            }
            if (!response.ok) throw new Error('반영하지 못했습니다. 초안 목록에서 상태를 확인하세요.');
            const result = await response.json();
            window.location.assign(result.url);
        } catch (error) {
            status.textContent = error.message;
            button.disabled = false;
        }
    });
})();
