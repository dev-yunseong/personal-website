package dev.yunseong.website.global.controller;

import dev.yunseong.website.ai.service.SuggestedQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SuggestedQuestionService suggestedQuestionService;

    @GetMapping("/public/chat")
    public String chat(Model model) {
        model.addAttribute("suggestedQuestions", suggestedQuestionService.getQuestions());
        return "chat";
    }
}
