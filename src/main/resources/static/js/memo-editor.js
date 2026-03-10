function initMemoEditor() {
    document.getElementById('writeTab').addEventListener('click', function() {
        document.getElementById('writePane').style.display = 'block';
        document.getElementById('previewPane').style.display = 'none';
        document.getElementById('writeTab').classList.add('active');
        document.getElementById('previewTab').classList.remove('active');
    });

    document.getElementById('previewTab').addEventListener('click', function() {
        document.getElementById('writePane').style.display = 'none';
        const previewPane = document.getElementById('previewPane');
        previewPane.style.display = 'block';
        previewPane.innerHTML = '<span class="text-muted">Loading preview...</span>';
        document.getElementById('previewTab').classList.add('active');
        document.getElementById('writeTab').classList.remove('active');

        const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        const formData = new FormData();
        formData.append('content', document.getElementById('content').value);

        fetch('/admin/memos/preview', {
            method: 'POST',
            headers: { [header]: token },
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
                    const content = code.textContent;
                    const div = document.createElement('div');
                    div.className = 'mermaid';
                    div.textContent = content;
                    if (code.parentElement && code.parentElement.tagName === 'PRE') {
                        code.parentElement.replaceWith(div);
                    } else {
                        code.replaceWith(div);
                    }
                });
                try {
                    await mermaid.run({ querySelector: '#previewPane .mermaid' });
                } catch (e) {
                    console.error('Mermaid error:', e);
                }
            }
        })
        .catch(error => {
            console.error('Preview failed:', error);
            previewPane.innerHTML = '<span class="text-danger">Failed to load preview.</span>';
        });
    });
}
