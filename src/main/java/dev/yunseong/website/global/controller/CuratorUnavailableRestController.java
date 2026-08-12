package dev.yunseong.website.global.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.yunseong.website.global.config.ConditionalOnCuratorDisabled;

/**
 * Stands in for {@code ChatRestController} while the curator is unavailable.
 *
 * <p>Without it the route would simply not exist, and a 404 reads like a broken deployment.
 * A 503 says the feature is off on purpose and may come back.
 */
@ConditionalOnCuratorDisabled
@RestController
public class CuratorUnavailableRestController {

    @GetMapping("/api/public/chat")
    public ResponseEntity<Map<String, String>> chatUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "큐레이터를 지금은 이용할 수 없습니다."));
    }
}
