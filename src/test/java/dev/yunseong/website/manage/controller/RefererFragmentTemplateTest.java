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
 * Renders the traffic sources fragment through the same selector
 * {@code console/dashboard.html} uses, so a broken fragment name or malformed
 * markup fails here rather than in the browser.
 */
class RefererFragmentTemplateTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void refererFragment_RendersTabPaneAndLoaderEntryPoint() {
        String html = templateEngine.process(
                new TemplateSpec("console/fragments/referer", Set.of("referer"), TemplateMode.HTML, null),
                new Context());

        assertTrue(html.contains("id=\"pane-referer\""), html);
        assertTrue(html.contains("id=\"referer-content\""), html);
        assertTrue(html.contains("window.loadReferer"), html);
        assertTrue(html.contains("/api/admin/console/referer"), html);
        assertTrue(html.contains("data-bots=\"true\""), html);
        assertTrue(html.contains("data-bots=\"false\""), html);
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
