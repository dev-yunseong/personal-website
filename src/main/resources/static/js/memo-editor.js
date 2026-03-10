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
        .then(html => { previewPane.innerHTML = html; })
        .catch(error => {
            console.error('Preview failed:', error);
            previewPane.innerHTML = '<span class="text-danger">Failed to load preview.</span>';
        });
    });
}
