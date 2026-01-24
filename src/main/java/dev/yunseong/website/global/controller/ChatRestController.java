package dev.yunseong.website.global.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.yunseong.website.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping(value = "/api/public/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam("message") String message,
            HttpServletRequest httpServletRequest
    ) {

        return chatService.getChatResponse(message, httpServletRequest.getRemoteAddr());
    }
}
