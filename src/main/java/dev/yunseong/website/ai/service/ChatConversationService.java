package dev.yunseong.website.ai.service;

import dev.yunseong.website.ai.domain.ChatConversation;
import dev.yunseong.website.ai.repository.ChatConversationRepository;
import dev.yunseong.website.manage.domain.IpLatestChat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatConversationService {

    private static final int PAGE_SIZE = 10;

    private final ChatConversationRepository chatConversationRepository;

    @Transactional
    public void save(String ip, String userMessage, String aiResponse, String toolsUsed) {
        chatConversationRepository.save(new ChatConversation(ip, userMessage, aiResponse, toolsUsed));
    }

    @Transactional(readOnly = true)
    public Page<ChatConversation> getConversationsByIp(String ip, int page) {
        return chatConversationRepository.findByIpOrderByCreatedAtDesc(ip, PageRequest.of(page, PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Page<IpLatestChat> getIpsSortedByLatest(int page) {
        return chatConversationRepository.findDistinctIpsOrderedByLatest(PageRequest.of(page, PAGE_SIZE));
    }
}
