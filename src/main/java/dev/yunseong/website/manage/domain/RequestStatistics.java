package dev.yunseong.website.manage.domain;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "request_statistics")
public class RequestStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String uri;

    @Column(nullable = false)
    private String method;

    @Column(length = 1024)
    private String referer;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column
    private String ip;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    public RequestStatistics(String uri, String method, String referer, String userAgent, String ipAddress) {
        this(null, uri, method, referer, userAgent, ipAddress, null);
    }
}
