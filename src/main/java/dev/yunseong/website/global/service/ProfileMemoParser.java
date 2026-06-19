package dev.yunseong.website.global.service;

import dev.yunseong.website.global.domain.Profile;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ProfileMemoParser {

    private static final int MAX_PROFILE_LENGTH = 200_000;

    public Profile parse(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Profile memo content is empty");
        }
        if (content.length() > MAX_PROFILE_LENGTH) {
            throw new IllegalArgumentException("Profile memo content is too large");
        }

        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(MAX_PROFILE_LENGTH);
        options.setMaxAliasesForCollections(20);
        options.setAllowDuplicateKeys(false);

        Object loaded;
        try {
            loaded = new Yaml(new SafeConstructor(options)).load(content);
        } catch (YAMLException e) {
            throw new IllegalArgumentException("Invalid YAML syntax: " + e.getMessage(), e);
        }
        Map<String, Object> root = asMap(loaded, "profile");

        return new Profile(
                requiredString(root, "name"),
                string(root, "headline"),
                string(root, "location"),
                requiredString(root, "about"),
                contacts(root.get("contacts")),
                projects(root.get("projects")),
                awards(root.get("awards")),
                education(root.get("education")),
                certificates(root.get("certificates")),
                assessments(root.get("assessments"))
        );
    }

    private List<Profile.Contact> contacts(Object value) {
        return mapList(value, "contacts").stream()
                .map(item -> new Profile.Contact(
                        requiredString(item, "label"),
                        safeUrl(requiredString(item, "url"))
                ))
                .toList();
    }

    private List<Profile.Project> projects(Object value) {
        return mapList(value, "projects").stream()
                .map(item -> new Profile.Project(
                        requiredString(item, "name"),
                        safeUrl(string(item, "url")),
                        string(item, "role"),
                        period(item.get("period")),
                        stringList(item.get("highlights"), "projects.highlights"),
                        stringList(item.get("technologies"), "projects.technologies")
                ))
                .toList();
    }

    private List<Profile.Award> awards(Object value) {
        return mapList(value, "awards").stream()
                .map(item -> new Profile.Award(
                        requiredString(item, "name"),
                        string(item, "result"),
                        string(item, "date"),
                        string(item, "organization"),
                        safeUrl(string(item, "url"))
                ))
                .toList();
    }

    private List<Profile.Education> education(Object value) {
        return mapList(value, "education").stream()
                .map(item -> new Profile.Education(
                        requiredString(item, "name"),
                        period(item.get("period")),
                        string(item, "description"),
                        safeUrl(string(item, "url"))
                ))
                .toList();
    }

    private List<Profile.Certificate> certificates(Object value) {
        return mapList(value, "certificates").stream()
                .map(item -> new Profile.Certificate(
                        requiredString(item, "name"),
                        string(item, "issuedAt"),
                        string(item, "issuer"),
                        safeUrl(string(item, "url"))
                ))
                .toList();
    }

    private List<Profile.Assessment> assessments(Object value) {
        return mapList(value, "assessments").stream()
                .map(item -> new Profile.Assessment(
                        requiredString(item, "name"),
                        string(item, "score"),
                        string(item, "level"),
                        string(item, "assessedAt"),
                        safeUrl(string(item, "url"))
                ))
                .toList();
    }

    private Profile.Period period(Object value) {
        if (value == null) {
            return null;
        }
        Map<String, Object> period = asMap(value, "period");
        return new Profile.Period(string(period, "start"), string(period, "end"));
    }

    private List<Map<String, Object>> mapList(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException(field + " must be a list");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : values) {
            result.add(asMap(item, field));
        }
        return result;
    }

    private List<String> stringList(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException(field + " must be a list");
        }
        return values.stream()
                .map(item -> item == null ? "" : item.toString().trim())
                .filter(item -> !item.isBlank())
                .toList();
    }

    private Map<String, Object> asMap(Object value, String field) {
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException(field + " must be a mapping");
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        source.forEach((key, item) -> result.put(key.toString(), item));
        return result;
    }

    private String requiredString(Map<String, Object> source, String field) {
        String value = string(source, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String string(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    private String safeUrl(String url) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("/")
                || url.startsWith("https://")
                || url.startsWith("http://")
                || url.startsWith("mailto:")) {
            return url;
        }
        throw new IllegalArgumentException("Unsupported profile URL: " + url);
    }
}
