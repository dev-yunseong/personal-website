package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestSummary;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders {@code console/requests.html} against a model shaped like the one
 * {@link RequestBrowseController} builds, so the list page is exercised as
 * markup rather than only as a view name.
 */
class ConsoleRequestsTemplateTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    private static RequestSummary summary() {
        return new RequestSummary(42L, LocalDateTime.of(2026, 7, 30, 12, 0, 5), "GET", "/public/memos",
                404, 12, "1.1.1.1", "https://news.example", "curl/8.0", true, 87,
                "datacenter,ua_mismatch", "KR");
    }

    private WebContext context(List<RequestBrowseController.ActiveFilter> activeFilters) {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication
                .buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        context.setVariable("requests", new PageImpl<>(List.of(summary()), PageRequest.of(0, 10), 1));
        context.setVariable("days", 7);
        context.setVariable("page", 0);
        context.setVariable("statusFilter", "");
        context.setVariable("uri", "");
        context.setVariable("ip", "");
        context.setVariable("userAgent", "");
        context.setVariable("referer", "");
        context.setVariable("countryCode", "");
        context.setVariable("activeFilters", activeFilters);
        context.setVariable("selfHref", "/admin/console/requests?days=7&uri=/public/memos");
        context.setVariable("returnHref", "/admin/console?tab=history");
        return context;
    }

    @Test
    void requestsPage_RendersOneRowPerRequestWithEveryStoredListColumn() {
        String html = templateEngine.process("console/requests", context(List.of()));

        assertTrue(html.contains("2026-07-30 12:00:05"));
        assertTrue(html.contains("/public/memos"));
        assertTrue(html.contains("1.1.1.1"));
        assertTrue(html.contains("https://news.example"));
        assertTrue(html.contains("curl/8.0"));
        assertTrue(html.contains("12ms"));
        assertTrue(html.contains("87 datacenter,ua_mismatch"));
        // A 4xx is warned, not just printed.
        assertTrue(html.contains("status--warn"));
    }

    @Test
    void requestsPage_LinksEachRowToThatRequestsDetailPageCarryingThisListAsFrom() {
        String html = templateEngine.process("console/requests", context(List.of()));

        assertTrue(html.contains("/admin/console/requests/42?from="));
        assertTrue(html.contains("console-row-link"));
        assertTrue(html.contains("console-row-clickable"));

        // The list's own path and query travel as one parameter and survive
        // the round trip, so the detail page's 돌아가기 returns to this list.
        Matcher href = Pattern.compile("/admin/console/requests/42\\?from=([^\"]+)").matcher(html);
        assertTrue(href.find());
        assertEquals("/admin/console/requests?days=7&uri=/public/memos",
                URLDecoder.decode(href.group(1), StandardCharsets.UTF_8));
    }

    @Test
    void requestsPage_ReturnLinkPointsAtTheModelsSanitisedDestination() {
        String html = templateEngine.process("console/requests", context(List.of()));

        assertTrue(html.contains("돌아가기"));
        assertTrue(html.contains("href=\"/admin/console?tab=history\""));
    }

    @Test
    void requestsPage_WithNoFilters_SaysSoInsteadOfShowingAnEmptyFilterLine() {
        String html = templateEngine.process("console/requests", context(List.of()));

        assertTrue(html.contains("필터 없이"));
        assertFalse(html.contains("활성 필터"));
    }

    @Test
    void requestsPage_StatesTheActiveFilterAndOffersTheLinkThatClearsIt() {
        String html = templateEngine.process("console/requests", context(List.of(
                new RequestBrowseController.ActiveFilter("URI", "/public/memos",
                        "/admin/console/requests?days=7"))));

        assertTrue(html.contains("활성 필터"));
        assertTrue(html.contains(">URI<"));
        assertTrue(html.contains(">/public/memos<"));
        assertTrue(html.contains("href=\"/admin/console/requests?days=7\""));
        assertFalse(html.contains("필터 없이"));
    }

    @Test
    void requestsPage_EscapesAFilterValueRatherThanRenderingItAsMarkup() {
        String html = templateEngine.process("console/requests", context(List.of(
                new RequestBrowseController.ActiveFilter("URI", "<script>alert(1)</script>",
                        "/admin/console/requests?days=7"))));

        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
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
