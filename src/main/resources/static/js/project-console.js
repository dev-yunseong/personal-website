document.querySelectorAll('[data-project-chart]').forEach(chart => {
    const rows = [...chart.querySelectorAll('tbody tr')];
    const values = rows.map(row => Number(row.querySelector('td').textContent));
    if (!values.length || !values.every(Number.isFinite)) return;
    const svg = chart.querySelector('svg');
    const namespace = 'http://www.w3.org/2000/svg';
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;
    const x = index => values.length === 1 ? 300 : 60 + index * 500 / (values.length - 1);
    const y = value => max === min ? 90 : 150 - (value - min) * 120 / range;
    const add = (tag, attributes, label) => {
        const element = document.createElementNS(namespace, tag);
        Object.entries(attributes).forEach(([key, value]) => element.setAttribute(key, value));
        if (label !== undefined) element.textContent = label;
        svg.append(element);
    };
    [min, max].filter((value, index, all) => all.indexOf(value) === index).forEach(value => {
        add('line', {x1: 60, y1: y(value), x2: 560, y2: y(value), class: 'chart-grid'});
        add('text', {x: 52, y: y(value) + 4, 'text-anchor': 'end'}, String(value));
    });
    add('polyline', {points: values.map((value, index) => `${x(index)},${y(value)}`).join(' '), class: 'chart-line'});
    values.forEach((value, index) => add('circle', {cx: x(index), cy: y(value), r: 3, class: 'chart-point'}));
    add('text', {x: 60, y: 185}, rows[0].querySelector('th').textContent);
    if (values.length > 1) add('text', {x: 560, y: 185, 'text-anchor': 'end'}, rows.at(-1).querySelector('th').textContent);
    svg.removeAttribute('hidden');
    chart.querySelector('details').open = false;
});
