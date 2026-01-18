package dev.yunseong.website.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    @GetMapping("/admin/chat")
    public String chat() {
        return "chat";
    }
}
