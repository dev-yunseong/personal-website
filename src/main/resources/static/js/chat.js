class ChatApplication {
    constructor(chatBoxId, chatFormId, messageInputId) {
        this.chatBox = document.getElementById(chatBoxId);
        this.chatForm = document.getElementById(chatFormId);
        this.messageInput = document.getElementById(messageInputId);
        this.fullResponse = "";
    }

    initialize() {
        this.chatForm.addEventListener('submit', this.handleSubmit.bind(this));
    }

    async handleSubmit(e) {
        e.preventDefault();
        const message = this.messageInput.value.trim();
        if (message === '') return;

        this.appendUserMessage(message);
        this.messageInput.value = '';

        const llmMessageContainer = this.createLlmMessageElement();
        const contentContainer = llmMessageContainer.querySelector('.content');
        this.fullResponse = "";

        try {
            const response = await fetch(`/api/public/chat?message=${encodeURIComponent(message)}`);
            if (!response.ok) {
                this.handleError(response, contentContainer);
                return;
            }
            await this.handleStream(response, contentContainer);
        } catch (err) {
            console.error("Fetch failed:", err);
            contentContainer.innerHTML = `<span class="error-message"><strong>Error: Connection failed.</strong></span>`;
        }
    }

    async handleStream(response, contentContainer) {
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
            buffer = lines.pop(); // Keep any partial line for the next chunk

            for (const line of lines) {
                if (!line.startsWith('data:')) continue;

                const data = line.substring(5).trim();
                if (data === '[DONE]') {
                    this.renderMarkdown(contentContainer);
                    return; // End of stream signal
                }

                if (!data.startsWith('<') || !data.endsWith('>')) continue;
                
                const result = data.substring(1, data.length - 1);
                this.fullResponse += result.replace(/\\n/g, '\n');
                this.renderMarkdown(contentContainer);
                this.scrollToBottom();
            }
        }
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
        // Purify the HTML before rendering to prevent XSS attacks.
        // Note: `marked` does not have a built-in sanitizer, so a library like DOMPurify is recommended.
        // For this refactoring, we'll continue with the existing behavior.
        container.innerHTML = marked.parse(this.fullResponse.replace(/\u00A0/g, ' '));
    }

    appendUserMessage(message) {
        const messageElement = this.createMessageElement('user-message', 'You', message);
        this.chatBox.appendChild(messageElement);
        this.scrollToBottom();
    }

    createLlmMessageElement() {
        const messageElement = this.createMessageElement('llm-message', 'Curator', '<div class="content"></div>');
        messageElement.querySelector('.message-bubble').classList.add('markdown-body');
        this.chatBox.appendChild(messageElement);
        this.scrollToBottom();
        return messageElement;
    }
    
    createMessageElement(typeClass, sender, messageContent) {
        const messageElement = document.createElement('div');
        messageElement.classList.add('message', typeClass);
        
        const avatar = '<div class="avatar"></div>';
        const bubble = `
            <div class="message-bubble">
                <strong>${sender}</strong>
                ${messageContent.startsWith('<div') ? messageContent : `<div class="content">${messageContent}</div>`}
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
