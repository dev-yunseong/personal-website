package dev.yunseong.website.global.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "persistent_logins")
@Getter
@NoArgsConstructor
public class LoginToken {
    @Id
    @Column(length = 64)
    private String series;
    @Column(nullable = false, length = 100)
    private String username;
    @Column(nullable = false, length = 64)
    private String token;
    @Column(name = "last_used", nullable = false)
    private Instant lastUsed;

    public LoginToken(String series, String username, String token, Instant lastUsed) {
        this.series = series;
        this.username = username;
        update(token, lastUsed);
    }

    public void update(String token, Instant lastUsed) {
        this.token = token;
        this.lastUsed = lastUsed;
    }
}
