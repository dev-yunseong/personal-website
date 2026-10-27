const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const vm = require('node:vm');

const source = readFileSync('src/main/resources/static/js/draft-review.js', 'utf8');

function setup(confirmed, response = {ok: true, json: async () => ({url: '/admin/console/drafts'})}) {
    let submit;
    const calls = [];
    const destinations = [];
    const buttons = [{disabled: false}, {disabled: false}];
    const status = {textContent: ''};
    const form = {
        dataset: {url: '/admin/console/drafts/2/approval'},
        elements: {csrfHeader: {value: 'X-CSRF-TOKEN'}, csrfToken: {value: 'token'}, revision: {value: 'revision'}},
        querySelectorAll: () => buttons,
        addEventListener: (_, listener) => submit = listener
    };
    vm.runInNewContext(source, {
        document: {getElementById: id => id === 'draft-approval' ? form : status},
        window: {confirm: () => confirmed, location: {assign: url => destinations.push(url)}},
        fetch: async (...args) => {calls.push(args); return response;}
    });
    const send = (rejecting = true) => submit({
        preventDefault() {},
        submitter: {value: rejecting ? 'reject' : '', dataset: {url: '/admin/console/drafts/2'}}
    });
    return {send, calls, destinations, buttons, status};
}

test('확인을 취소하면 초안을 삭제하지 않는다', async () => {
    const page = setup(false);
    await page.send();
    assert.equal(page.calls.length, 0);
    assert.ok(page.buttons.every(button => !button.disabled));
});

test('확인하면 CSRF와 revision을 전송하고 목록으로 이동한다', async () => {
    const page = setup(true);
    await page.send();
    const [url, request] = page.calls[0];
    assert.equal(url, '/admin/console/drafts/2');
    assert.equal(request.method, 'DELETE');
    assert.equal(request.headers['X-CSRF-TOKEN'], 'token');
    assert.deepEqual(JSON.parse(request.body), {revision: 'revision'});
    assert.deepEqual(page.destinations, ['/admin/console/drafts']);
    await page.send();
    assert.equal(page.calls.length, 1);
});

test('충돌하면 이동하지 않고 오류와 재시도 버튼을 표시한다', async () => {
    const page = setup(true, {ok: false, status: 409});
    await page.send();
    assert.deepEqual(page.destinations, []);
    assert.match(page.status.textContent, /다시 검토/);
    assert.ok(page.buttons.every(button => !button.disabled));
});

test('승인 요청의 경로와 메서드를 유지한다', async () => {
    const page = setup(false);
    await page.send(false);
    assert.equal(page.calls[0][0], '/admin/console/drafts/2/approval');
    assert.equal(page.calls[0][1].method, 'PUT');
});
