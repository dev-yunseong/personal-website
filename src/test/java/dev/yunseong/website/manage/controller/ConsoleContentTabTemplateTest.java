package dev.yunseong.website.manage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleContentTabTemplateTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void contentTab_RendersPaneAndLoaderHook() {
        String html = templateEngine.process("console/fragments/content",
                java.util.Set.of("contentTab"), new Context());

        assertTrue(html.contains("id=\"pane-content\""));
        assertTrue(html.contains("id=\"content-body\""));
        assertTrue(html.contains("window.loadContentStats"));
        assertTrue(html.contains("/api/admin/console/content"));
    }

    @Test
    void dashboard_IncludesContentTabFragment() {
        // dashboard.html uses @{...} link expressions, so it needs a web context.
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication
                .buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        context.setVariable("days", 7);
        context.setVariable("tab", "content");

        String html = templateEngine.process("console/dashboard", context);

        assertTrue(html.contains("data-tab=\"content\""));
        assertTrue(html.contains("id=\"pane-content\""));
        assertTrue(html.contains("loadContentStats(STATE.days)"));
        // the fragment's own script must travel with the included pane
        assertTrue(html.contains("window.loadContentStats"));
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
