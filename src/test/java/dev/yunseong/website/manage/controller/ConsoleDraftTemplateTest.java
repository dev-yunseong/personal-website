package dev.yunseong.website.manage.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoDraftService;
import dev.yunseong.website.blog.service.MemoLineDiff;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleDraftTemplateTest {
    @Test
    void reviewRendersEscapedDiffAndRevision() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        MockServletContext servlet = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication.buildApplication(servlet)
                .buildExchange(new MockHttpServletRequest(servlet), new MockHttpServletResponse()));
        Memo draft = new Memo(2L, "/private/draft/notes/a", "<script>alert(1)</script>\n새 본문", null, null);
        Memo current = new Memo(1L, "/notes/a", "이전 본문", null, null);
        context.setVariable("review", new MemoDraftService.Review(draft, "/notes/a", current, "review-hash",
                MemoLineDiff.compare(current.getContent(), draft.getContent())));
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-csrf"));
        String html = engine.process("console/draft_review", context);
        assertThat(html).contains("class=\"removed\"", "class=\"added\"", "review-hash", "test-csrf",
                "/admin/console/drafts/2/approval", "&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(html).contains("기각하고 삭제", "value=\"reject\"", "data-url=\"/admin/console/drafts/2\"");
        assertThat(html).doesNotContain("<script>alert(1)</script>");
    }
}
