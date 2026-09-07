package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestDetail;
import org.junit.jupiter.api.Test;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders {@code console/request_detail.html}. The page's whole point is that
 * every stored field is shown, whole and labelled, so the assertions walk the
 * four groups and then check the two ways a field can be absent: an unresolved
 * lookup and a column that was never written.
 */
class ConsoleRequestDetailTemplateTest {

    private static final String LONG_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) "
                    + "Version/17.1 Safari/605.1.15";

    private final SpringTemplateEngine templateEngine = templateEngine();

    private static RequestDetail full() {
        return new RequestDetail(42L, LocalDateTime.of(2026, 7, 30, 12, 0, 5), "GET",
                "/public/memos/1?q=spring", 200, 12, "1.1.1.1", "https://news.example", LONG_USER_AGENT,
                true, 87, "datacenter,ua_mismatch", "KR", "Seoul", 37.56, 126.98, 20,
                15169L, "Google LLC");
    }

    private static RequestDetail bare() {
        return new RequestDetail(7L, LocalDateTime.of(2026, 7, 30, 12, 0, 5), "GET", "/", null, null,
                "10.0.0.1", null, null, false, null, null, null, null, null, null, null, null, null);
    }

    private String render(RequestDetail detail, String returnHref) {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication
                .buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        context.setVariable("detail", detail);
        context.setVariable("returnHref", returnHref);
        return templateEngine.process("console/request_detail", context);
    }

    @Test
    void detailPage_GroupsEveryStoredFieldUnderItsKoreanHeading() {
        String html = render(full(), "/admin/console?tab=history");

        assertTrue(html.contains("요청"));
        assertTrue(html.contains("출처"));
        assertTrue(html.contains("클라이언트"));
        assertTrue(html.contains("Bot 판정"));

        assertTrue(html.contains("2026-07-30 12:00:05"));
        assertTrue(html.contains("GET"));
        assertTrue(html.contains("200"));
        assertTrue(html.contains("12ms"));
        assertTrue(html.contains("1.1.1.1"));
        assertTrue(html.contains("KR"));
        assertTrue(html.contains("Seoul"));
        assertTrue(html.contains("87"));
        assertTrue(html.contains("datacenter,ua_mismatch"));
    }

    @Test
    void detailPage_ShowsTheUriRefererAndUserAgentWhole() {
        String html = render(full(), "/admin/console?tab=history");

        assertTrue(html.contains("/public/memos/1?q=spring"));
        assertTrue(html.contains("https://news.example"));
        assertTrue(html.contains(LONG_USER_AGENT));
        // Full width, not the list's one-line clip.
        assertTrue(html.contains("req-detail__wrap"));
        assertFalse(html.contains("cell-clip"));
    }

    @Test
    void detailPage_LabelsTheCoordinatesAsAnApproximationWithItsRadius() {
        String html = render(full(), "/admin/console?tab=history");

        assertTrue(html.contains("37.56, 126.98"));
        assertTrue(html.contains("오차 반경 최대 20km"));
        assertTrue(html.contains("MaxMind 근사 위치"));
    }

    @Test
    void detailPage_WritesTheNetworkAsAsNumberThenOrganisation() {
        String html = render(full(), "/admin/console?tab=history");

        assertTrue(html.contains("AS15169 Google LLC"));
    }

    @Test
    void detailPage_WithNothingResolved_RendersAnEmDashAndNeverTheWordNull() {
        String html = render(bare(), "/admin/console?tab=history");

        assertFalse(html.contains("null"));
        assertTrue(html.contains("—"));
        // Bot 여부 always has an answer, even when nothing else does.
        assertTrue(html.contains("아니오"));
    }

    @Test
    void detailPage_ReturnLinkPointsAtTheModelsSanitisedDestination() {
        String html = render(full(), "/admin/console/requests?days=7&uri=/public/memos");

        assertTrue(html.contains("돌아가기"));
        assertTrue(html.contains("href=\"/admin/console/requests?days=7&amp;uri=/public/memos\""));
    }

    @Test
    void detailPage_OffersTheRequestListFilteredByThisRequestsOwnValues() {
        String html = render(full(), "/admin/console?tab=history");

        assertTrue(html.contains("같은 값의 요청 보기"));
        assertTrue(html.contains("ip=1.1.1.1"));

        // The URI carries its own query string into the link's uri parameter
        // without either half being lost.
        Matcher href = Pattern.compile("/admin/console/requests\\?days=30&amp;uri=([^&\"]+)").matcher(html);
        assertTrue(href.find(), html);
        assertEquals("/public/memos/1?q=spring", URLDecoder.decode(href.group(1), StandardCharsets.UTF_8));
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
