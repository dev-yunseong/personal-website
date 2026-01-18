package dev.yunseong.website.ai.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.yunseong.apilimitmvc.domain.LimitRule;

@Configuration
public class ApiLimitConfig {

    private final String AI_API_PATH = "/api/public/chat";

    @Bean
    LimitRule<String> singleFactor() {
        return new LimitRule<>(
                AI_API_PATH,
                100,
                Duration.ofDays(1),
                new SingleFactor()
        );
    }
}
