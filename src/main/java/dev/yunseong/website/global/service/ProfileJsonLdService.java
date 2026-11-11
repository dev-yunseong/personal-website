package dev.yunseong.website.global.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.global.domain.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfileJsonLdService {

    private static final String SITE_NAME = "Yunseong";

    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ProfileJsonLdService(ObjectMapper objectMapper, @Value("${app.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public String toJsonLd(Profile profile, String imageUrl) {
        String personId = baseUrl + "/#person";

        Map<String, Object> person = new LinkedHashMap<>();
        person.put("@type", "Person");
        person.put("@id", personId);
        person.put("name", profile.name());
        person.put("url", baseUrl);
        putIfPresent(person, "jobTitle", profile.headline());
        putIfPresent(person, "description", profile.about());
        putIfPresent(person, "image", imageUrl);
        List<String> sameAs = externalProfileUrls(profile);
        if (!sameAs.isEmpty()) {
            person.put("sameAs", sameAs);
        }

        Map<String, Object> website = new LinkedHashMap<>();
        website.put("@type", "WebSite");
        website.put("@id", baseUrl + "/#website");
        website.put("url", baseUrl);
        website.put("name", SITE_NAME);
        website.put("inLanguage", "ko");
        website.put("author", Map.of("@id", personId));

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("@context", "https://schema.org");
        document.put("@graph", List.of(person, website));

        try {
            // template 이 이 값을 <script> 안에 escape 없이 넣으므로, 프로필 본문에
            // "</script>" 가 있어도 script 가 끊기지 않게 "<" 를 JSON escape 로 바꾼다.
            return objectMapper.writeValueAsString(document).replace("<", "\\u003c");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize profile JSON-LD", e);
        }
    }

    // mailto 같은 연락처는 sameAs 가 요구하는 프로필 페이지 URL 이 아니라서 뺀다.
    private List<String> externalProfileUrls(Profile profile) {
        return profile.contacts().stream()
                .map(Profile.Contact::url)
                .filter(url -> url != null && (url.startsWith("https://") || url.startsWith("http://")))
                .toList();
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
