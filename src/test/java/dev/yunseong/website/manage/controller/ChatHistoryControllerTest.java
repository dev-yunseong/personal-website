package dev.yunseong.website.manage.controller;

import dev.yunseong.website.ai.domain.ChatConversation;
import dev.yunseong.website.ai.service.ChatConversationService;
import dev.yunseong.website.manage.domain.IpLatestChat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatHistoryControllerTest {

    @Mock
    private Model model;

    @Mock
    private ChatConversationService chatConversationService;

    @InjectMocks
    private ChatHistoryController chatHistoryController;

    @Test
    void chatHistory_NoIpSelected_ReturnsChatHistoryView() {
        Page<IpLatestChat> emptyPage = Page.empty();
        when(chatConversationService.getIpsSortedByLatest(0)).thenReturn(emptyPage);

        String view = chatHistoryController.chatHistory(model, null, 0, 0);

        assertEquals("console/chat_history", view);
    }

    @Test
    void chatHistory_NoIpSelected_AddsIpListToModel() {
        IpLatestChat entry = new IpLatestChat("1.2.3.4", LocalDateTime.now());
        Page<IpLatestChat> ipPage = new PageImpl<>(List.of(entry));
        when(chatConversationService.getIpsSortedByLatest(0)).thenReturn(ipPage);

        chatHistoryController.chatHistory(model, null, 0, 0);

        verify(model).addAttribute("ipList", ipPage);
        verify(model).addAttribute("selectedIp", (String) null);
        verify(model).addAttribute("ipPage", 0);
        verify(chatConversationService, never()).getConversationsByIp(anyString(), anyInt());
    }

    @Test
    void chatHistory_WithIpSelected_AddsConversationsToModel() {
        Page<IpLatestChat> emptyIpPage = Page.empty();
        when(chatConversationService.getIpsSortedByLatest(0)).thenReturn(emptyIpPage);

        Page<ChatConversation> convPage = new PageImpl<>(List.of());
        when(chatConversationService.getConversationsByIp("1.2.3.4", 0)).thenReturn(convPage);

        chatHistoryController.chatHistory(model, "1.2.3.4", 0, 0);

        verify(model).addAttribute("conversations", convPage);
        verify(model).addAttribute("selectedIp", "1.2.3.4");
        verify(model).addAttribute("page", 0);
    }

    @Test
    void chatHistory_WithBlankIp_DoesNotFetchConversations() {
        Page<IpLatestChat> emptyPage = Page.empty();
        when(chatConversationService.getIpsSortedByLatest(0)).thenReturn(emptyPage);

        chatHistoryController.chatHistory(model, "  ", 0, 0);

        verify(chatConversationService, never()).getConversationsByIp(anyString(), anyInt());
    }
}
