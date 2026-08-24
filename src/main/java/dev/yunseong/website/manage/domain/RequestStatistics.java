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

    /** 45 characters is the longest IPv6 literal, including an IPv4-mapped suffix. */
    @Column(length = 45)
    private String ip;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "is_bot", nullable = false)
    private boolean isBot;

    @Column(name = "duration_ms")
    private Integer durationMs;

    /** ISO 3166-1 alpha-2, resolved from {@link #ip} at insert time. Null when unresolved. */
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "city_geoname_id")
    private Long cityGeoNameId;

    @Column(name = "city_name", length = 255)
    private String cityName;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "accuracy_radius_km")
    private Integer accuracyRadiusKm;

    /**
     * Evidence total behind {@link #isBot}, from {@link BotDetector}. Null on
     * rows written before the scoring model existed; headers are not stored, so
     * those can never be backfilled.
     */
    @Column(name = "bot_score")
    private Integer botScore;

    /** Comma-separated names of the signals that fired, for tuning the weights. */
    @Column(name = "bot_signals", length = 128)
    private String botSignals;

    public RequestStatistics(String uri, String method, String referer, String userAgent, String ipAddress) {
        this(null, uri, method, referer, userAgent, ipAddress, null, null, false, null,
                null, null, null, null, null, null, null, null);
    }

    public RequestStatistics(String uri, String method, String referer, String userAgent, String ipAddress, Integer statusCode) {
        this(null, uri, method, referer, userAgent, ipAddress, statusCode, null, false, null,
                null, null, null, null, null, null, null, null);
    }

    public RequestStatistics(String uri, String method, String referer, String userAgent, String ipAddress,
                             Integer statusCode, boolean isBot, Integer durationMs) {
        this(uri, method, referer, userAgent, ipAddress, statusCode, isBot, durationMs, (String) null);
    }

    public RequestStatistics(String uri, String method, String referer, String userAgent, String ipAddress,
                             Integer statusCode, boolean isBot, Integer durationMs, String countryCode) {
        this(null, uri, method, referer, userAgent, ipAddress, statusCode, null, isBot, durationMs,
                countryCode, null, null, null, null, null, null, null);
    }

    /**
     * The request-path constructor: stores the decision together with the
     * evidence that produced it, so the weights can be tuned from recorded data.
     */
    public RequestStatistics(String uri, String method, String referer, String userAgent, String ipAddress,
                             Integer statusCode, BotVerdict verdict, Integer durationMs, GeoLocation location) {
        this(null, uri, method, referer, userAgent, ipAddress, statusCode, null, verdict.bot(), durationMs,
                location == null ? null : location.countryCode(),
                location == null ? null : location.cityGeoNameId(),
                location == null ? null : location.cityName(),
                location == null ? null : location.latitude(),
                location == null ? null : location.longitude(),
                location == null ? null : location.accuracyRadiusKm(),
                verdict.score(), verdict.signals());
    }
}
