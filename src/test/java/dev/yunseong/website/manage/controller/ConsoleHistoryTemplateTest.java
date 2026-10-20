package dev.yunseong.website.manage.controller;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Request History card in {@code console/dashboard.html}: the row
 * rendering itself lives inside an inline {@code <script>} (built at runtime
 * from {@code GET /api/admin/console/history}), which a raw markup-selector
 * pass cannot exercise meaningfully — so this test only asserts on the static
 * markup a fragment selector can genuinely reach: the card shell, the
 * click-to-detail help tooltip, and the still-present status
 * filter and content mount point the existing script wires up.
 *
 * The accordion this once guarded (a toggle button plus a hidden detail row)
 * is gone as of #199: Request History rows now link straight to
 * {@code /admin/console/requests/{id}}, built inside that same inline
 * script, so it is likewise outside what a fragment selector can reach.
 */
class ConsoleHistoryTemplateTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void requestHistoryPane_RendersCardShellAndDetailHelp() {
        String html = templateEngine.process("console/dashboard", Set.of("div#pane-history"), new Context());

        assertTrue(html.contains("id=\"pane-history\""));
        assertTrue(html.contains("Request History"));
        assertTrue(html.contains("id=\"history-content\""));

        // Guidance remains available through a focusable help button.
        assertTrue(html.contains("<button type=\"button\" class=\"info-help\""));
        assertTrue(html.contains("aria-label=\"요청 목록 도움말\""));
        assertTrue(html.contains("data-help=\"행을 클릭하면 그 요청의 상세 페이지가 열립니다.\""));
        assertFalse(html.contains("panel-note"));

        // Status filter untouched by this change.
        assertTrue(html.contains("js-hist-status"));
    }

    @Test
    void dashboardTabContent_IncludesRequestHistoryPane() {
        String html = templateEngine.process("console/dashboard", Set.of("div.tab-content"), new Context());

        assertTrue(html.contains("id=\"pane-history\""));
        assertTrue(html.contains("id=\"history-content\""));
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
