package dev.yunseong.website.global.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.global.domain.Profile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfileJsonLdServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProfileJsonLdService service = new ProfileJsonLdService(objectMapper, "https://yunseong.dev");

    @Test
    void toJsonLd_DescribesPersonAndWebsite() throws Exception {
        Profile profile = profile("Builds maintainable systems.", List.of(
                new Profile.Contact("GitHub", "https://github.com/dev-yunseong"),
                new Profile.Contact("Email", "mailto:someone@example.com")
        ));

        JsonNode graph = objectMapper.readTree(service.toJsonLd(profile, "https://cdn.example.com/profile.png"))
                .get("@graph");

        JsonNode person = graph.get(0);
        assertEquals("Person", person.get("@type").asText());
        assertEquals("Yunseong Jeong", person.get("name").asText());
        assertEquals("https://yunseong.dev", person.get("url").asText());
        assertEquals("Software Engineer", person.get("jobTitle").asText());
        assertEquals("https://cdn.example.com/profile.png", person.get("image").asText());
        assertEquals(1, person.get("sameAs").size());
        assertEquals("https://github.com/dev-yunseong", person.get("sameAs").get(0).asText());

        JsonNode website = graph.get(1);
        assertEquals("WebSite", website.get("@type").asText());
        assertEquals(person.get("@id").asText(), website.get("author").get("@id").asText());
    }

    @Test
    void toJsonLd_OmitsBlankFields() throws Exception {
        Profile profile = profile(null, List.of());

        JsonNode person = objectMapper.readTree(service.toJsonLd(profile, "")).get("@graph").get(0);

        assertFalse(person.has("description"));
        assertFalse(person.has("image"));
        assertFalse(person.has("sameAs"));
    }

    @Test
    void toJsonLd_EscapesScriptClosingTag() throws Exception {
        Profile profile = profile("</script><script>alert(1)</script>", List.of());

        String jsonLd = service.toJsonLd(profile, null);

        assertFalse(jsonLd.contains("<"));
        assertEquals("</script><script>alert(1)</script>",
                objectMapper.readTree(jsonLd).get("@graph").get(0).get("description").asText());
    }

    private Profile profile(String about, List<Profile.Contact> contacts) {
        return new Profile(
                "Yunseong Jeong",
                "Software Engineer",
                "Busan, KR",
                about,
                contacts,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
