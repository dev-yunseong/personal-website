package dev.yunseong.website.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "tools_used", length = 1024)
    private String toolsUsed;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    public ChatConversation(String ip, String userMessage, String aiResponse, String toolsUsed) {
        this.ip = ip;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.toolsUsed = toolsUsed;
    }
}
