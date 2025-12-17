package com.yunseong.website.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
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

    @Column(name = "request_count", nullable = false)
    private Long requestCount;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    public RequestStatistics(String uri, String method, Long requestCount) {
        this.uri = uri;
        this.method = method;
        this.requestCount = requestCount;
    }
}
