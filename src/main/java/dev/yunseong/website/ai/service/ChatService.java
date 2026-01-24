package dev.yunseong.website.ai.service;

import dev.yunseong.website.ai.domain.BlogAgent;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final BlogAgent blogAgent;

    public Flux<String> getChatResponse(String message, String conversationId) {
        return blogAgent.prompt(message, conversationId)
                .map(token -> token.replace(" ", "\u00A0"))
                .concatWith(Flux.just("[DONE]"));
    }
}
