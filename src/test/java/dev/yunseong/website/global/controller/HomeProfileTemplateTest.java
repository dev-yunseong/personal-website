package dev.yunseong.website.global.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.global.domain.Profile;
import dev.yunseong.website.global.domain.ProfileLoadResult;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeProfileTemplateTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void homeHero_RendersProfileContent() {
        Profile profile = new Profile(
                "Yunseong Jeong",
                "Software Engineer",
                "Busan, KR",
                "Builds maintainable systems.",
                List.of(new Profile.Contact("GitHub", "https://github.com/dev-yunseong")),
                List.of(new Profile.Project(
                        "Maechuri",
                        "/public/memos/70",
                        "Full-stack & AI",
                        new Profile.Period("2025-12", "2026-03"),
                        List.of("Built an AI mystery game."),
                        List.of("Spring Boot", "RAG")
                )),
                List.of(new Profile.Award("Example Award", "Winner", "2026", null, null)),
                List.of(new Profile.Education(
                        "Pusan National University",
                        new Profile.Period("2021-03", null),
                        null,
                        null
                )),
                List.of(new Profile.Certificate("SQLD", "2026-03-27", null, null)),
                List.of(new Profile.Assessment("TOPCIT", "717", "Level 4", "2026-06-15", null))
        );
        Context context = new Context();
        context.setVariable("profile", profile);
        context.setVariable("profileImageUrl", "");

        String html = templateEngine.process("fragments/home_hero_fragment", context);

        assertTrue(html.contains("Yunseong Jeong"));
        assertTrue(html.contains("Selected projects"));
        assertTrue(html.contains("Maechuri"));
        assertTrue(html.contains("Certificates"));
        assertTrue(html.contains("TOPCIT"));
    }

    @Test
    void homeHero_RendersFallbackWhenProfileIsMissing() {
        Context context = new Context();
        context.setVariable("profile", null);
        context.setVariable("profileImageUrl", "");
        context.setVariable("profileStatus", ProfileLoadResult.Status.MISSING);

        String html = templateEngine.process("fragments/home_hero_fragment", context);

        assertTrue(html.contains("Profile could not be displayed."));
        assertTrue(html.contains("has not been created yet"));
    }

    @Test
    void homeHero_RendersInvalidProfileError() {
        Context context = new Context();
        context.setVariable("profile", null);
        context.setVariable("profileImageUrl", "");
        context.setVariable("profileStatus", ProfileLoadResult.Status.INVALID);

        String html = templateEngine.process("fragments/home_hero_fragment", context);

        assertTrue(html.contains("contains invalid YAML"));
    }

    @Test
    void recentMemosSection_RendersRecentMemoLinks() {
        Context context = new Context();
        context.setVariable("recentMemos", List.of(
                new Memo(10L, "/notes/recent", "Recent content", LocalDateTime.now(), LocalDateTime.of(2026, 6, 25, 10, 30)),
                new Memo(11L, "/dev/spring", "Spring content", LocalDateTime.now(), LocalDateTime.of(2026, 6, 24, 10, 30))
        ));

        String html = templateEngine.process("fragments/home_recent_memos_fragment", context);

        assertTrue(html.contains("Recent memos"));
        assertTrue(html.contains("/public/memos/10"));
        assertTrue(html.contains("recent"));
        assertTrue(html.contains("/notes"));
        assertTrue(html.contains("2026-06-25"));
        assertTrue(html.contains("/public/memos"));
    }

    @Test
    void recentMemosSection_RendersEmptyState() {
        Context context = new Context();
        context.setVariable("recentMemos", List.of());

        String html = templateEngine.process("fragments/home_recent_memos_fragment", context);

        assertTrue(html.contains("No public memos yet."));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
