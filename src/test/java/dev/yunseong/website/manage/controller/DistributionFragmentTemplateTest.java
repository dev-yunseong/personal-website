package dev.yunseong.website.manage.controller;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolves the fragment through the same selector {@code console/dashboard.html}
 * uses, so a renamed fragment or a template the parser chokes on fails here
 * instead of at runtime.
 */
class DistributionFragmentTemplateTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void distribution_RendersPaneAndBotToggle() {
        String html = templateEngine.process(
                new TemplateSpec("console/fragments/distribution", Set.of("distribution"), TemplateMode.HTML, null),
                new Context());

        assertTrue(html.contains("id=\"pane-distribution\""));
        assertTrue(html.contains("id=\"distribution-content\""));
        assertTrue(html.contains("js-dist-bots"));
        assertTrue(html.contains("window.loadDistribution"));
        assertTrue(html.contains("/api/admin/console/distribution"));
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
