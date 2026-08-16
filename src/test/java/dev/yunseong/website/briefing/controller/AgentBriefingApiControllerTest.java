package dev.yunseong.website.briefing.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.briefing.service.BriefingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgentBriefingApiControllerTest {

    private static final MediaType TEXT_PLAIN_UTF8 =
            new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    @Mock
    private BriefingService briefingService;

    @InjectMocks
    private AgentBriefingApiController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "baseUrl", "https://yunseong.dev");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static Memo briefing(String kind, String date, String content) {
        return new Memo(1L, "/private/briefing/" + kind + "/" + date, content, null, null);
    }

    @Test
    void publish_StoresTheBodyVerbatimAndAnswersWithTheDigestUrl() throws Exception {
        when(briefingService.publish(eq("news"), any(), anyString()))
                .thenReturn(briefing("news", "2026-08-12", "stored"));

        mockMvc.perform(post("/api/agent/briefings/news")
                        .param("date", "2026-08-12")
                        .contentType(TEXT_PLAIN_UTF8)
                        .content("# 오늘의 뉴스\n\n본문\n"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("https://yunseong.dev/briefing?date=2026-08-12\n"));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(briefingService).publish(eq("news"), eq(LocalDate.of(2026, 8, 12)), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).isEqualTo("# 오늘의 뉴스\n\n본문\n");
    }

    @Test
    void publish_WithoutDateUsesToday() throws Exception {
        when(briefingService.publish(eq("news"), any(), anyString()))
                .thenReturn(briefing("news", LocalDate.now().toString(), "stored"));

        mockMvc.perform(post("/api/agent/briefings/news")
                        .contentType(TEXT_PLAIN_UTF8)
                        .content("# 제목\n\n본문"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://yunseong.dev/briefing?date=" + LocalDate.now() + "\n"));

        verify(briefingService).publish("news", LocalDate.now(), "# 제목\n\n본문");
    }

    /**
     * {@code text/plain} with no charset means ISO-8859-1 to Spring's string converter,
     * which would silently turn a Korean briefing into question marks. UTF-8 is the
     * assumption a client that did not say gets.
     */
    @Test
    void publish_DecodesAUtf8BodySentWithoutACharset() throws Exception {
        when(briefingService.publish(eq("news"), any(), anyString()))
                .thenReturn(briefing("news", "2026-08-12", "stored"));

        mockMvc.perform(post("/api/agent/briefings/news")
                        .param("date", "2026-08-12")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("# 오늘의 뉴스\n\n본문\n".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk());

        verify(briefingService).publish("news", LocalDate.of(2026, 8, 12), "# 오늘의 뉴스\n\n본문\n");
    }

    /** An explicit charset on the request still wins over the UTF-8 assumption. */
    @Test
    void publish_HonoursAnExplicitRequestCharset() throws Exception {
        when(briefingService.publish(eq("news"), any(), anyString()))
                .thenReturn(briefing("news", "2026-08-12", "stored"));

        mockMvc.perform(post("/api/agent/briefings/news")
                        .param("date", "2026-08-12")
                        .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_16))
                        .content("# 오늘의 뉴스".getBytes(StandardCharsets.UTF_16)))
                .andExpect(status().isOk());

        verify(briefingService).publish("news", LocalDate.of(2026, 8, 12), "# 오늘의 뉴스");
    }

    /**
     * A new kind needs no registration anywhere — posting under the name is the whole
     * ceremony, and the controller must not have an allow-list of its own.
     */
    @Test
    void publish_AcceptsAKindTheServerHasNeverSeen() throws Exception {
        when(briefingService.publish(eq("weather-alerts"), any(), anyString()))
                .thenReturn(briefing("weather-alerts", "2026-08-12", "stored"));

        mockMvc.perform(post("/api/agent/briefings/weather-alerts")
                        .param("date", "2026-08-12")
                        .contentType(TEXT_PLAIN_UTF8)
                        .content("# 날씨\n\n본문"))
                .andExpect(status().isOk());

        verify(briefingService).publish(eq("weather-alerts"), any(), anyString());
    }

    @Test
    void publish_AnswersBadRequestWhenTheServiceRejectsTheKind() throws Exception {
        when(briefingService.publish(anyString(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("Kind must be lowercase letters, digits, and hyphens (1-32 characters)."));

        mockMvc.perform(post("/api/agent/briefings/NOPE")
                        .contentType(TEXT_PLAIN_UTF8)
                        .content("# 제목\n\n본문"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kind must be lowercase")));
    }

    @Test
    void publish_AnswersBadRequestForAMalformedDateWithoutCallingTheService() throws Exception {
        mockMvc.perform(post("/api/agent/briefings/news")
                        .param("date", "2026/08/12")
                        .contentType(TEXT_PLAIN_UTF8)
                        .content("# 제목\n\n본문"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("yyyy-MM-dd")));

        verify(briefingService, never()).publish(anyString(), any(), anyString());
    }

    @Test
    void publish_AnswersBadRequestForAnEmptyBody() throws Exception {
        when(briefingService.publish(anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("Briefing body is required."));

        mockMvc.perform(post("/api/agent/briefings/news")
                        .contentType(TEXT_PLAIN_UTF8)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("required")));
    }

    @Test
    void historical_ReturnsTheStoredContentAsUtf8PlainText() throws Exception {
        when(briefingService.findByKindAndDate("news", LocalDate.of(2026, 8, 12)))
                .thenReturn(Optional.of(briefing("news", "2026-08-12", "# 지난 뉴스\n\n본문")));

        mockMvc.perform(get("/api/agent/briefings/news/2026-08-12"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TEXT_PLAIN_UTF8))
                .andExpect(content().string("# 지난 뉴스\n\n본문"));

        verify(briefingService).findByKindAndDate("news", LocalDate.of(2026, 8, 12));
    }

    @Test
    void historical_AnswersNotFoundWithKindAndDate() throws Exception {
        when(briefingService.findByKindAndDate("news", LocalDate.of(2026, 8, 11)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/agent/briefings/news/2026-08-11"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(TEXT_PLAIN_UTF8))
                .andExpect(content().string(
                        "No briefing published for kind 'news' on 2026-08-11.\n"));
    }

    @Test
    void historical_AnswersBadRequestForAMalformedDateWithoutCallingTheService() throws Exception {
        mockMvc.perform(get("/api/agent/briefings/news/2026-08-15-extra"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(TEXT_PLAIN_UTF8))
                .andExpect(content().string("Date must be yyyy-MM-dd.\n"));

        verify(briefingService, never()).findByKindAndDate(anyString(), any());
    }

    @Test
    void latest_ReturnsTheStoredContentAsPlainText() throws Exception {
        when(briefingService.findLatest("news"))
                .thenReturn(Optional.of(briefing("news", "2026-08-12", "# 오늘의 뉴스\n\n본문")));

        mockMvc.perform(get("/api/agent/briefings/news/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("# 오늘의 뉴스\n\n본문"));

        verify(briefingService, never()).findByKindAndDate(anyString(), any());
    }

    @Test
    void latest_AnswersNotFoundForAKindWithNothingPublished() throws Exception {
        when(briefingService.findLatest("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/agent/briefings/unknown/latest"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("unknown")));
    }

    @Test
    void kinds_ListsPublishedKindsOnePerLine() throws Exception {
        when(briefingService.listKinds()).thenReturn(List.of("jobs", "news"));

        mockMvc.perform(get("/api/agent/briefings/kinds"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("jobs\nnews\n"));
    }

    @Test
    void kinds_IsEmptyRatherThanABlankLineWhenNothingIsPublished() throws Exception {
        when(briefingService.listKinds()).thenReturn(List.of());

        mockMvc.perform(get("/api/agent/briefings/kinds"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    /** {@code /kinds} is one segment and {@code /{kind}/latest} is two — no collision. */
    @Test
    void kinds_DoesNotShadowTheLatestRoute() throws Exception {
        when(briefingService.findLatest("kinds"))
                .thenReturn(Optional.of(briefing("kinds", "2026-08-12", "a kind literally named kinds")));

        mockMvc.perform(get("/api/agent/briefings/kinds/latest"))
                .andExpect(status().isOk())
                .andExpect(content().string("a kind literally named kinds"));

        verify(briefingService, never()).listKinds();
    }
}
