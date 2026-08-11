package dev.yunseong.website.global.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    private final boolean curatorEnabled;

    public ChatController(@Value("${app.curator.enabled:true}") boolean curatorEnabled) {
        this.curatorEnabled = curatorEnabled;
    }

    @GetMapping("/public/chat")
    public String chat(Model model) {
        // The page still renders when the curator is off; the fragment swaps the composer for a
        // notice, so a visitor is never handed an input that cannot work.
        model.addAttribute("curatorEnabled", curatorEnabled);
        return "chat";
    }
}
