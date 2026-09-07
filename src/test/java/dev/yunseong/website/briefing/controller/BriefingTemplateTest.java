package dev.yunseong.website.briefing.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.briefing.domain.BriefingEntry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders {@code briefing.html} on its own, without the layout decorator or the security
 * dialect, so the assertions are about this template's own markup.
 */
class BriefingTemplateTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    private final SpringTemplateEngine templateEngine = templateEngine();

    private static BriefingEntry entry(long id, String kind, String body) {
        return new BriefingEntry(kind,
                new Memo(id, "/private/briefing/" + kind + "/2026-08-12", body, null, null));
    }

    private String render(List<BriefingEntry> entries, List<LocalDate> recentDates) {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication
                .buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        context.setVariable("selectedDate", DATE);
        context.setVariable("entries", entries);
        context.setVariable("recentDates", recentDates);
        return templateEngine.process("briefing", context);
    }

    @Test
    void rendersEveryBriefingBodyExpanded() {
        String html = render(
                List.of(entry(1L, "jobs", "# 오늘의 채용\n\n- 지원 마감 임박\n"),
                        entry(2L, "news", "# 오늘의 뉴스\n\n첫 소식입니다.\n")),
                List.of(DATE));

        assertThat(html)
                .contains("오늘의 채용")
                .contains("지원 마감 임박")
                .contains("오늘의 뉴스")
                .contains("첫 소식입니다.");
    }

    /**
     * fragments/mermaid_katex_parser.html resolves exactly one {@code #markdown-content},
     * so a day of briefings has to share a single container. One per entry would leave
     * every briefing after the first without math rendering.
     */
    @Test
    void putsTheWholeDayInsideOneMarkdownContentContainer() {
        String html = render(
                List.of(entry(1L, "jobs", "# 채용\n\n본문"), entry(2L, "news", "# 뉴스\n\n본문")),
                List.of(DATE));

        assertThat(html.split("id=\"markdown-content\"", -1)).hasSize(2);
        assertThat(html).contains("markdown-body");
    }

    @Test
    void reusesTheMemoArticleReadingSurface() {
        String html = render(List.of(entry(1L, "news", "# 뉴스\n\n본문")), List.of(DATE));

        assertThat(html)
                .contains("/css/memo-view.css")
                .contains("/css/briefing.css")
                .contains("memo-article__body");
    }

    @Test
    void givesEachBriefingAnAnchorNamedForItsKind() {
        String html = render(
                List.of(entry(1L, "jobs", "# 채용\n\n본문"), entry(2L, "news", "# 뉴스\n\n본문")),
                List.of(DATE));

        assertThat(html).contains("id=\"briefing-jobs\"").contains("id=\"briefing-news\"");
        assertThat(html).contains("#briefing-jobs").contains("#briefing-news");
    }

    @Test
    void hidesTheKindJumpListWhenOnlyOneBriefingArrived() {
        String html = render(List.of(entry(1L, "news", "# 뉴스\n\n본문")), List.of(DATE));

        assertThat(html).doesNotContain("briefing-kinds__item");
    }

    /**
     * The stored memo is private, so the link 404s for a signed-out visitor. It has to stay
     * behind {@code sec:authorize} rather than being rendered for everyone.
     */
    @Test
    void gatesTheSourceMemoLinkBehindAuthentication() {
        String html = render(List.of(entry(42L, "news", "# 뉴스\n\n본문")), List.of(DATE));

        assertThat(html).contains("/public/memos/42");
        assertThat(html).containsPattern("(?s)<a[^>]*sec:authorize=\"isAuthenticated\\(\\)\"[^>]*"
                + "class=\"briefing-entry__source\"");
    }

    @Test
    void rendersAnEmptyStateForADayWithNoBriefings() {
        String html = render(List.of(), List.of(DATE));

        assertThat(html).contains("이 날짜에는 브리핑이 없습니다.");
        assertThat(html).doesNotContain("id=\"markdown-content\"");
    }

    @Test
    void linksToOtherDaysAndMarksTheCurrentOne() {
        String html = render(
                List.of(entry(1L, "news", "# 뉴스\n\n본문")),
                List.of(DATE, LocalDate.of(2026, 8, 11)));

        assertThat(html)
                .contains("/briefing?date=2026-08-12")
                .contains("/briefing?date=2026-08-11")
                .contains("briefing-dates__link--current")
                .contains("aria-current=\"page\"");
    }

    @Test
    void showsTheSelectedDateInTheHeader() {
        String html = render(List.of(entry(1L, "news", "# 뉴스\n\n본문")), List.of(DATE));

        assertThat(html).contains("<time datetime=\"2026-08-12\">2026-08-12</time>");
    }

    @Test
    void loadsTheMathAndDiagramParser() {
        String html = render(List.of(entry(1L, "news", "# 뉴스\n\n본문")), List.of(DATE));

        assertThat(html).contains("katex").contains("mermaid");
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
