package dev.yunseong.website.global.service;

import dev.yunseong.website.global.domain.Profile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileMemoParserTest {

    private final ProfileMemoParser parser = new ProfileMemoParser();

    @Test
    void parse_MapsProfileSectionsAndPreservesOrder() {
        String yaml = """
                name: Yunseong Jeong
                headline: Software Engineer
                location: Busan, KR
                about: Builds maintainable systems.
                contacts:
                  - label: GitHub
                    url: https://github.com/dev-yunseong
                projects:
                  - name: First project
                    url: /public/memos/1
                    period:
                      start: "2026-01"
                      end:
                    highlights:
                      - First result
                    technologies:
                      - Spring Boot
                  - name: Second project
                    period:
                      start: "2025-01"
                      end: "2025-02"
                awards:
                  - name: Example award
                    result: Winner
                    date: "2026"
                education: []
                certificates:
                  - name: SQLD
                    issuedAt: "2026-03-27"
                assessments:
                  - name: TOPCIT
                    score: "717"
                    level: Level 4
                    assessedAt: "2026-06-15"
                """;

        Profile profile = parser.parse(yaml);

        assertEquals("Yunseong Jeong", profile.name());
        assertEquals("GitHub", profile.contacts().getFirst().label());
        assertEquals("First project", profile.projects().getFirst().name());
        assertEquals("Second project", profile.projects().get(1).name());
        assertNull(profile.projects().getFirst().period().end());
        assertTrue(profile.projects().getFirst().period().ongoing());
        assertEquals("SQLD", profile.certificates().getFirst().name());
        assertEquals("717", profile.assessments().getFirst().score());
    }

    @Test
    void parse_RejectsUnsupportedUrlSchemes() {
        String yaml = """
                name: Yunseong Jeong
                about: About
                contacts:
                  - label: Unsafe
                    url: javascript:alert(1)
                """;

        assertThrows(IllegalArgumentException.class, () -> parser.parse(yaml));
    }

    @Test
    void parse_RequiresNameAndAbout() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("name: Yunseong Jeong"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("about: About"));
    }

    @Test
    void parse_ReportsMalformedYamlAsValidationError() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("name: [broken")
        );

        assertTrue(error.getMessage().startsWith("Invalid YAML syntax:"));
    }

    @Test
    void parse_ProfileExampleIsValid() throws IOException {
        String yaml = Files.readString(Path.of("profile.example.yml"));

        Profile profile = parser.parse(yaml);

        assertEquals("Yunseong Jeong", profile.name());
        assertEquals(5, profile.projects().size());
        assertEquals(9, profile.awards().size());
        assertEquals(4, profile.education().size());
        assertEquals(2, profile.certificates().size());
        assertEquals(1, profile.assessments().size());
    }
}
