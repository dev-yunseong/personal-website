package dev.yunseong.website.manage.controller;

import dev.yunseong.website.ai.domain.ChatConversation;
import dev.yunseong.website.ai.service.ChatConversationService;
import dev.yunseong.website.manage.domain.IpLatestChat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatConversationService chatConversationService;

    @GetMapping
    public String chatHistory(Model model,
                              @RequestParam(required = false) String ip,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "0") int ipPage) {
        Page<IpLatestChat> ipList = chatConversationService.getIpsSortedByLatest(ipPage);
        model.addAttribute("ipList", ipList);
        model.addAttribute("ipPage", ipPage);
        model.addAttribute("selectedIp", ip);

        if (ip != null && !ip.isBlank()) {
            Page<ChatConversation> conversations = chatConversationService.getConversationsByIp(ip, page);
            model.addAttribute("conversations", conversations);
            model.addAttribute("page", page);
        }

        return "console/chat_history";
    }
}
