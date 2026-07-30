package dev.yunseong.website.manage.controller;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the fragment selector {@code console/fragments/visitors :: pane} that
 * {@code console/dashboard.html} includes, and the approximation notice the issue
 * requires next to the metrics.
 */
class VisitorsFragmentTemplateTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void visitorsPane_RendersShellAndApproximationNotice() {
        String html = templateEngine.process("console/fragments/visitors", Set.of("pane"), new Context());

        assertTrue(html.contains("id=\"pane-visitors\""));
        assertTrue(html.contains("id=\"visitors-content\""));
        assertTrue(html.contains("근사치"));
        assertTrue(html.contains("30분 무활동"));
        assertTrue(html.contains("봇 제외"));
        assertTrue(html.contains("window.loadVisitors"));
        assertTrue(html.contains("/api/admin/console/visitors"));
    }

    @Test
    void dashboardTabContent_IncludesVisitorsPane() {
        String html = templateEngine.process("console/dashboard", Set.of("div.tab-content"), new Context());

        assertTrue(html.contains("id=\"pane-visitors\""));
        assertTrue(html.contains("window.loadVisitors"));
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
