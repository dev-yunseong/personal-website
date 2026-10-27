(() => {
    const form = document.getElementById('draft-approval');
    if (!form) return;
    let pending = false;
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (pending) return;
        const rejecting = event.submitter?.value === 'reject';
        if (rejecting && !window.confirm('이 초안을 기각하고 삭제할까요? 삭제한 초안은 복구할 수 없습니다.')) return;
        const buttons = form.querySelectorAll('button');
        const status = document.getElementById('approval-status');
        pending = true;
        buttons.forEach(button => button.disabled = true);
        status.textContent = rejecting ? '삭제 중…' : '반영 중…';
        try {
            const response = await fetch(rejecting ? event.submitter.dataset.url : form.dataset.url, {
                method: rejecting ? 'DELETE' : 'PUT',
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
            if (!response.ok) throw new Error(rejecting
                ? '삭제하지 못했습니다. 초안 목록에서 상태를 확인하세요.'
                : '반영하지 못했습니다. 초안 목록에서 상태를 확인하세요.');
            const result = await response.json();
            window.location.assign(result.url);
        } catch (error) {
            status.textContent = error.message;
            pending = false;
            buttons.forEach(button => button.disabled = false);
        }
    });
})();
