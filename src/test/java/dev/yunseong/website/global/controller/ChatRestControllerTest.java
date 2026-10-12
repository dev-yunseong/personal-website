package dev.yunseong.website.global.controller;

import dev.yunseong.website.ai.service.ChatService;
import dev.yunseong.website.global.util.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatRestControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatRestController chatRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatRestController).build();
    }

    @Test
    void chat_ReturnsSseStreamFromChatService() throws Exception {
        when(chatService.getChatResponse("hello", "127.0.0.1"))
                .thenReturn(Flux.just("data:chunk1\n\n", "data:chunk2\n\n"));

        MvcResult mvcResult = mockMvc.perform(get("/api/public/chat").param("message", "hello"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

        verify(chatService).getChatResponse("hello", "127.0.0.1");
    }

    @Test
    void chat_ResolvesClientIpFromCloudflareHeader() throws Exception {
        when(chatService.getChatResponse(eq("hello"), eq("203.0.113.195")))
                .thenReturn(Flux.just("data:test\n\n"));

        MvcResult mvcResult = mockMvc.perform(get("/api/public/chat")
                        .param("message", "hello")
                        .header(ClientIpResolver.CLOUDFLARE_HEADER, "203.0.113.195"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        verify(chatService).getChatResponse("hello", "203.0.113.195");
    }
}
