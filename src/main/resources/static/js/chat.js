class ChatApplication {
    constructor(chatBoxId, chatFormId, messageInputId) {
        this.chatBox = document.getElementById(chatBoxId);
        this.chatForm = document.getElementById(chatFormId);
        this.messageInput = document.getElementById(messageInputId);
        this.submitButton = this.chatForm ? this.chatForm.querySelector('button[type="submit"]') : null;
        this.fullResponse = "";
        this.isStreaming = false;
    }

    initialize() {
        // The composer is not rendered when the curator is unavailable. Without this the
        // constructor and this call would both throw on every load of the chat page.
        if (!this.chatForm || !this.messageInput || !this.chatBox) return;
        this.chatForm.addEventListener('submit', this.handleSubmit.bind(this));
    }

    async handleSubmit(e) {
        e.preventDefault();
        if (this.isStreaming) return;
        const message = this.messageInput.value.trim();
        if (message === '') return;

        this.appendUserMessage(message);
        this.messageInput.value = '';
        this.setInputDisabled(true);
        this.isStreaming = true;

        const { messageElement, contentContainer, toolStatusEl } = this.createLlmMessageElement();
        this.fullResponse = "";

        const toolHistory = [];
        const streamState = { hasText: false };
        let ellipsisInterval = null;

        try {
            const response = await fetch(`/api/public/chat?message=${encodeURIComponent(message)}`);
            if (!response.ok) {
                this.handleError(response, contentContainer);
                return;
            }
            ellipsisInterval = this.startEllipsisAnimation(toolStatusEl.querySelector('.tool-current-dots'));
            await this.handleStream(response, messageElement, contentContainer, toolStatusEl, toolHistory, streamState);
        } catch (err) {
            console.error("Fetch failed:", err);
            contentContainer.innerHTML = `<span class="error-message"><strong>Error: Connection failed.</strong></span>`;
        } finally {
            if (ellipsisInterval) clearInterval(ellipsisInterval);
            this.finalizeToolStatus(toolStatusEl, toolHistory, streamState);
            this.setInputDisabled(false);
            this.isStreaming = false;
        }
    }

    async handleStream(response, messageElement, contentContainer, toolStatusEl, toolHistory, streamState) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) {
                this.renderMarkdown(contentContainer);
                break;
            }

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop();

            for (const line of lines) {
                if (!line.startsWith('data:')) continue;

                const data = line.substring(5).trim();
                if (data === '[DONE]') {
                    this.renderMarkdown(contentContainer);
                    return;
                }

                try {
                    const event = JSON.parse(data);
                    if (event.type === 'text') {
                        if (!streamState.hasText) {
                            streamState.hasText = true;
                            if (toolHistory.length === 0) {
                                toolStatusEl.style.display = 'none';
                            }
                        }
                        this.fullResponse += event.content;
                        this.renderMarkdown(contentContainer);
                        this.scrollToBottom();
                    } else if (event.type === 'tool') {
                        toolHistory.push(event.content);
                        this.updateToolCurrent(toolStatusEl, event.content);
                        this.scrollToBottom();
                    } else if (event.type === 'message') {
                        this.insertSendMessage(messageElement, event.content);
                        this.scrollToBottom();
                    }
                } catch (e) {
                    console.warn('Failed to parse stream event:', data, e);
                }
            }
        }
    }

    startEllipsisAnimation(dotsEl) {
        if (!dotsEl) return null;
        let count = 3;
        return setInterval(() => {
            count = (count % 3) + 1;
            dotsEl.textContent = '.'.repeat(count);
        }, 400);
    }

    updateToolCurrent(toolStatusEl, toolContent) {
        const currentLabel = toolStatusEl.querySelector('.tool-current-label');
        if (currentLabel) {
            currentLabel.textContent = toolContent;
        }
        toolStatusEl.style.display = 'block';
    }

    setInputDisabled(disabled) {
        this.messageInput.disabled = disabled;
        if (this.submitButton) {
            this.submitButton.disabled = disabled;
        }
    }

    finalizeToolStatus(toolStatusEl, toolHistory, streamState) {
        const inProgress = toolStatusEl.querySelector('.tool-in-progress');
        if (toolHistory.length > 0) {
            toolStatusEl.style.display = 'block';
            if (inProgress) inProgress.style.display = 'none';
            const historyEl = toolStatusEl.querySelector('.tool-history');
            const summaryEl = toolStatusEl.querySelector('.tool-history-summary');
            const listEl = toolStatusEl.querySelector('.tool-history-list');
            if (summaryEl) summaryEl.textContent = `사용된 도구 (${toolHistory.length})`;
            if (listEl) {
                listEl.innerHTML = '';
                toolHistory.forEach(t => {
                    const li = document.createElement('li');
                    li.textContent = t;
                    listEl.appendChild(li);
                });
            }
            if (historyEl) historyEl.style.display = 'block';
        } else if (streamState.hasText) {
            toolStatusEl.style.display = 'none';
        } else {
            toolStatusEl.style.display = 'block';
        }
    }

    insertSendMessage(placeholderElement, content) {
        const msgEl = document.createElement('div');
        msgEl.classList.add('message', 'llm-message', 'send-message');
        msgEl.innerHTML = `
            <div class="avatar"></div>
            <div class="message-bubble markdown-body">
                <strong>Curator</strong>
                <div class="content">${marked.parse(content.replace(/\u00A0/g, ' '))}</div>
            </div>`;
        this.chatBox.insertBefore(msgEl, placeholderElement);
    }

    handleError(response, container) {
        let errorText;
        if (response.status === 429) {
            errorText = "<strong>Error: Too many requests. Please try again later.</strong>";
        } else {
            errorText = `<strong>Error: Connection failed with status ${response.status}.</strong>`;
        }
        container.innerHTML = `<span class="error-message">${errorText}</span>`;
    }

    renderMarkdown(container) {
        container.innerHTML = marked.parse(this.fullResponse.replace(/\u00A0/g, ' '));
    }

    appendUserMessage(message) {
        const messageElement = this.createMessageElement('user-message', 'You', message);
        this.chatBox.appendChild(messageElement);
        this.scrollToBottom();
    }

    createLlmMessageElement() {
        const messageElement = document.createElement('div');
        messageElement.classList.add('message', 'llm-message');

        const toolStatusHtml = `
            <div class="tool-status" role="status">
                <div class="tool-in-progress">
                    <span class="tool-current-label">Thinking</span><span class="tool-current-dots">...</span>
                </div>
                <details class="tool-history" style="display:none;">
                    <summary class="tool-history-summary">사용된 도구 (0)</summary>
                    <ul class="tool-history-list"></ul>
                </details>
            </div>`;

        messageElement.innerHTML = `
            <div class="avatar"></div>
            <div class="message-bubble markdown-body">
                <strong>Curator</strong>
                ${toolStatusHtml}
                <div class="content"></div>
            </div>`;

        this.chatBox.appendChild(messageElement);
        this.scrollToBottom();

        return {
            messageElement,
            contentContainer: messageElement.querySelector('.content'),
            toolStatusEl: messageElement.querySelector('.tool-status')
        };
    }

    createMessageElement(typeClass, sender, messageContent) {
        const messageElement = document.createElement('div');
        messageElement.classList.add('message', typeClass);

        const avatar = '<div class="avatar"></div>';
        const bubble = `
            <div class="message-bubble">
                <strong>${sender}</strong>
                <div class="content">${messageContent}</div>
            </div>`;

        messageElement.innerHTML = (typeClass === 'user-message') ? bubble + avatar : avatar + bubble;
        return messageElement;
    }

    scrollToBottom() {
        this.chatBox.scrollTop = this.chatBox.scrollHeight;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const chatApp = new ChatApplication('chat-box', 'chat-form', 'message-input');
    chatApp.initialize();
});
