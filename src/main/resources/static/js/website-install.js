(function () {
    'use strict';

    const command = document.getElementById('website-install-command');
    const copyButton = document.getElementById('website-install-copy');
    const feedback = document.getElementById('website-install-feedback');
    if (!command || !copyButton || !feedback) return;

    function selectCommand() {
        command.focus();
        const selection = window.getSelection();
        if (selection) {
            const range = document.createRange();
            range.selectNodeContents(command);
            selection.removeAllRanges();
            selection.addRange(range);
            feedback.textContent = '자동 복사를 사용할 수 없어 명령을 선택했습니다. Ctrl+C 또는 macOS의 Command+C로 복사하세요.';
            return;
        }
        feedback.textContent = '자동 복사를 사용할 수 없습니다. 위 명령을 직접 선택해 복사하세요.';
    }

    copyButton.addEventListener('click', async function () {
        feedback.textContent = '';
        if (!navigator.clipboard || typeof navigator.clipboard.writeText !== 'function') {
            selectCommand();
            return;
        }

        copyButton.disabled = true;
        try {
            await navigator.clipboard.writeText(command.textContent);
            feedback.textContent = '설치 명령을 복사했습니다. 터미널에 붙여넣어 실행하세요.';
        } catch (error) {
            selectCommand();
        } finally {
            copyButton.disabled = false;
        }
    });
})();
