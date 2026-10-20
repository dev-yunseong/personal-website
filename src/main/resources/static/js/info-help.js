(() => {
    // Delegation also covers analytics cards inserted after page load.
    new bootstrap.Tooltip(document.body, {
        selector: '.info-help',
        title() { return this.dataset.help; },
        container: 'body',
        customClass: 'info-help-tooltip',
        trigger: 'hover focus',
        html: false
    });

    // Touch browsers do not consistently focus buttons when tapped.
    document.addEventListener('click', event => {
        const button = event.target.closest('.info-help');
        if (button) button.focus();
    });

    document.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;
        document.querySelectorAll('.info-help[aria-describedby]').forEach(button => {
            bootstrap.Tooltip.getInstance(button)?.hide();
        });
    });
})();
