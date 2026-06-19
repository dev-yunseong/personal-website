function initMemoEditor(options = {}) {
    const profileMemo = Boolean(options.profileMemo);
    const content = document.getElementById('content');
    const writeTab = document.getElementById('writeTab');
    const previewTab = document.getElementById('previewTab');
    const writePane = document.getElementById('writePane');
    const previewPane = document.getElementById('previewPane');
    const validationStatus = document.getElementById('profileValidationStatus');

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        return { [header]: token };
    }

    function showValidation(message, state) {
        if (!validationStatus) return;

        validationStatus.textContent = message;
        validationStatus.classList.remove('alert-secondary', 'alert-info', 'alert-success', 'alert-danger');
        validationStatus.classList.add(`alert-${state}`);
    }

    async function validateProfileYaml(target = validationStatus) {
        if (target) {
            target.style.display = 'block';
            target.textContent = 'Validating profile YAML...';
            target.classList?.remove('alert-secondary', 'alert-success', 'alert-danger');
            target.classList?.add('alert-info');
        }

        const formData = new FormData();
        formData.append('content', content.value);

        try {
            const response = await fetch('/admin/memos/profile/validate', {
                method: 'POST',
                headers: csrfHeaders(),
                body: formData
            });
            const result = await response.json();
            const state = result.valid ? 'success' : 'danger';

            showValidation(result.message, state);
            if (target && target !== validationStatus) {
                target.textContent = result.message;
                target.className = `alert alert-${state}`;
            }
            return result.valid;
        } catch (error) {
            const message = `Validation failed: ${error.message}`;
            showValidation(message, 'danger');
            if (target && target !== validationStatus) {
                target.textContent = message;
                target.className = 'alert alert-danger';
            }
            return false;
        }
    }

    writeTab.addEventListener('click', function() {
        writePane.style.display = 'block';
        previewPane.style.display = 'none';
        writeTab.classList.add('active');
        previewTab.classList.remove('active');
    });

    previewTab.addEventListener('click', async function() {
        writePane.style.display = 'none';
        previewPane.style.display = 'block';
        previewTab.classList.add('active');
        writeTab.classList.remove('active');

        if (profileMemo) {
            await validateProfileYaml(previewPane);
            return;
        }

        previewPane.innerHTML = '<span class="text-muted">Loading preview...</span>';

        const formData = new FormData();
        formData.append('content', content.value);

        fetch('/admin/memos/preview', {
            method: 'POST',
            headers: csrfHeaders(),
            body: formData
        })
        .then(response => response.text())
        .then(async html => {
            previewPane.innerHTML = html;

            if (window.renderMathInElement) {
                renderMathInElement(previewPane, {
                    delimiters: [
                        {left: '$$', right: '$$', display: true},
                        {left: '$', right: '$', display: false}
                    ],
                    throwOnError: false
                });
            }

            const mermaidBlocks = previewPane.querySelectorAll('code.language-mermaid');
            if (window.mermaid && mermaidBlocks.length > 0) {
                mermaidBlocks.forEach((code) => {
                    const blockContent = code.textContent;
                    const div = document.createElement('div');
                    div.className = 'mermaid';
                    div.textContent = blockContent;
                    if (code.parentElement && code.parentElement.tagName === 'PRE') {
                        code.parentElement.replaceWith(div);
                    } else {
                        code.replaceWith(div);
                    }
                });
                try {
                    await mermaid.run({ querySelector: '#previewPane .mermaid' });
                } catch (error) {
                    console.error('Mermaid error:', error);
                }
            }
        })
        .catch(error => {
            console.error('Preview failed:', error);
            previewPane.innerHTML = '<span class="text-danger">Failed to load preview.</span>';
        });
    });

    if (profileMemo) {
        previewTab.textContent = 'Validate YAML';
        let validationTimer;
        content.addEventListener('input', function() {
            showValidation('Profile YAML changed. Waiting to validate...', 'secondary');
            window.clearTimeout(validationTimer);
            validationTimer = window.setTimeout(() => validateProfileYaml(), 500);
        });
        validateProfileYaml();
    }
}
