package dev.yunseong.website.manage.controller;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The console dashboard pulls this fragment in with th:replace, so a broken
 * fragment breaks the whole admin console page.
 */
class ConsoleGeoFragmentTest {

    @Test
    void geoFragment_RendersCountryPaneAndLoader() {
        String html = templateEngine().process("console/fragments/geo", new Context());

        assertTrue(html.contains("id=\"pane-geo\""), "the dashboard toggles the pane by id");
        assertTrue(html.contains("Requests by Country"));
        assertTrue(html.contains("id=\"geo-content\""));
        assertTrue(html.contains("/api/admin/console/geo/countries"));
        assertTrue(html.contains("/api/admin/console/geo/backfill"));
        assertTrue(html.contains("window.loadGeo"), "the dashboard tab dispatch calls window.loadGeo");
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
