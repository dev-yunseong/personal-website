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
        previewPane.innerHTML = DOMPurify.sanitize(marked.parse(document.getElementById('content').value));
        document.getElementById('previewTab').classList.add('active');
        document.getElementById('writeTab').classList.remove('active');
        if (window.Prism) {
            Prism.highlightAllUnder(previewPane);
        }
    });
}
