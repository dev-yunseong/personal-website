//package com.yunseong.website.controller;
//
//import com.yunseong.website.service.SitemapService;
//import jakarta.servlet.http.HttpServletRequest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SitemapControllerTest {
//
//    @Mock
//    private SitemapService sitemapService;
//
//    @Mock
//    private HttpServletRequest request;
//
//    @InjectMocks
//    private SitemapController sitemapController;
//
//    @BeforeEach
//    void setUp() {
//        when(request.getScheme()).thenReturn("http");
//        when(request.getServerName()).thenReturn("localhost");
//        when(request.getServerPort()).thenReturn(8080);
//    }
//
//    @Test
//    void sitemap_ReturnsXmlContent() {
//        // Given
//        String expectedSitemap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
//                "  <url>\n" +
//                "    <loc>http://localhost:8080</loc>\n" +
//                "  </url>\n" +
//                "</urlset>";
//        when(sitemapService.generateSitemap("http://localhost:8080")).thenReturn(expectedSitemap);
//
//        // When
//        String result = sitemapController.sitemap(request);
//
//        // Then
//        assertEquals(expectedSitemap, result);
//        verify(sitemapService).generateSitemap("http://localhost:8080");
//    }
//
//    @Test
//    void sitemap_HandlesHttpsWithDefaultPort() {
//        // Given
//        when(request.getScheme()).thenReturn("https");
//        when(request.getServerPort()).thenReturn(443);
//
//        String expectedSitemap = "<urlset></urlset>";
//        when(sitemapService.generateSitemap("https://localhost")).thenReturn(expectedSitemap);
//
//        // When
//        String result = sitemapController.sitemap(request);
//
//        // Then
//        assertEquals(expectedSitemap, result);
//        verify(sitemapService).generateSitemap("https://localhost");
//    }
//
//    @Test
//    void sitemap_HandlesHttpWithDefaultPort() {
//        // Given
//        when(request.getScheme()).thenReturn("http");
//        when(request.getServerPort()).thenReturn(80);
//
//        String expectedSitemap = "<urlset></urlset>";
//        when(sitemapService.generateSitemap("http://localhost")).thenReturn(expectedSitemap);
//
//        // When
//        String result = sitemapController.sitemap(request);
//
//        // Then
//        assertEquals(expectedSitemap, result);
//        verify(sitemapService).generateSitemap("http://localhost");
//    }
//
//    @Test
//    void sitemap_IncludesNonStandardPort() {
//        // Given
//        when(request.getServerPort()).thenReturn(8080);
//
//        String expectedSitemap = "<urlset></urlset>";
//        when(sitemapService.generateSitemap("http://localhost:8080")).thenReturn(expectedSitemap);
//
//        // When
//        String result = sitemapController.sitemap(request);
//
//        // Then
//        assertEquals(expectedSitemap, result);
//        verify(sitemapService).generateSitemap("http://localhost:8080");
//    }
//}
