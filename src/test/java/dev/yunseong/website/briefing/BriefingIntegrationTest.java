package dev.yunseong.website.briefing;

import dev.yunseong.website.ai.tool.BlogTools;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import dev.yunseong.website.blog.service.S3StorageService;
import dev.yunseong.website.global.config.BriefingTokenFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole path, through the running application: an agent posts a briefing, the memo
 * lands private, and the digest page shows the day while the public listings stay clean.
 *
 * <p>The leak checks are the reason this test exists at the integration level. The digest
 * page renders memos the visibility filter would refuse, so "does a briefing show up where
 * it must not" cannot be answered by unit-testing either side alone.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.briefing.token=integration-test-token",
    "app.base-url=http://localhost:8080",
    "s3.endpoint=http://localhost:9000",
    "s3.region=us-east-1",
    "s3.access-key=test",
    "s3.secret-key=test",
    "s3.bucket-name=test-bucket",
    "spring.datasource.url=jdbc:h2:mem:briefingdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    // Its own database, so it also has to create its own schema: application-db.yml
    // sets ddl-auto=none for the real PostgreSQL, where Flyway owns the schema.
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.security.user.name=admin",
    "spring.security.user.password=admin"
})
class BriefingIntegrationTest {

    private static final String TOKEN = "integration-test-token";
    private static final String TODAY = LocalDate.now().toString();

    @MockitoBean
    private BlogTools blogTools;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3StorageService s3StorageService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemoRepository memoRepository;

    @BeforeEach
    void clearMemos() {
        memoRepository.deleteAll();
    }

    private MvcResult publish(String kind, String body, String token) throws Exception {
        var request = post("/api/agent/briefings/" + kind)
                .contentType(MediaType.TEXT_PLAIN)
                .content(body.getBytes(StandardCharsets.UTF_8));
        if (token != null) {
            request = request.header(BriefingTokenFilter.TOKEN_HEADER, token);
        }
        return mockMvc.perform(request).andReturn();
    }

    private String bodyOf(MvcResult result) throws Exception {
        result.getResponse().setCharacterEncoding(StandardCharsets.UTF_8.name());
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void publishDay() throws Exception {
        publish("news", "# 오늘의 뉴스\n\n첫 소식입니다.\n", TOKEN);
        publish("jobs", "# 오늘의 채용\n\n지원 마감 임박.\n", TOKEN);
    }

    // ----------------------------------------------------------------- intake

    @Test
    void publishWithoutTokenIsUnauthorized() throws Exception {
        assertThat(publish("news", "# 제목\n\n본문", null).getResponse().getStatus()).isEqualTo(401);
        assertThat(memoRepository.count()).isZero();
    }

    @Test
    void publishWithWrongTokenIsUnauthorized() throws Exception {
        assertThat(publish("news", "# 제목\n\n본문", "nope").getResponse().getStatus()).isEqualTo(401);
        assertThat(memoRepository.count()).isZero();
    }

    /** No CSRF token, no session, no login — a cron job posts with one header. */
    @Test
    void publishWithTokenStoresAPrivateMemoWithoutCsrfOrLogin() throws Exception {
        MvcResult result = publish("news", "# 오늘의 뉴스\n\n첫 소식입니다.\n", TOKEN);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(bodyOf(result)).isEqualTo("http://localhost:8080/briefing?date=" + TODAY + "\n");

        Memo memo = memoRepository.findByName("/private/briefing/news/" + TODAY).orElseThrow();
        assertThat(memo.isPrivate()).isTrue();
        assertThat(memo.getContent())
                .contains("# 오늘의 뉴스")
                .contains("`news` · " + TODAY + " · 에이전트 작성")
                .contains("첫 소식입니다.");
    }

    @Test
    void republishingTheSameKindAndDayUpdatesRatherThanAddingAPost() throws Exception {
        publish("news", "# 오늘의 뉴스\n\n1차 원고\n", TOKEN);
        publish("news", "# 오늘의 뉴스\n\n2차 원고\n", TOKEN);

        assertThat(memoRepository.count()).isEqualTo(1);
        Memo memo = memoRepository.findByName("/private/briefing/news/" + TODAY).orElseThrow();
        assertThat(memo.getContent()).contains("2차 원고").doesNotContain("1차 원고");
    }

    /** Adding a kind is one POST — nothing is registered anywhere first. */
    @Test
    void aBrandNewKindNeedsNoRegistration() throws Exception {
        assertThat(publish("weather-alerts", "# 날씨\n\n호우 주의보\n", TOKEN)
                .getResponse().getStatus()).isEqualTo(200);

        assertThat(memoRepository.findByName("/private/briefing/weather-alerts/" + TODAY)).isPresent();

        MvcResult kinds = mockMvc.perform(get("/api/agent/briefings/kinds")
                        .header(BriefingTokenFilter.TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(bodyOf(kinds)).isEqualTo("weather-alerts\n");
    }

    @Test
    void latestReturnsTheStoredBriefingWithKoreanIntact() throws Exception {
        publish("news", "# 오늘의 뉴스\n\n첫 소식입니다.\n", TOKEN);

        MvcResult result = mockMvc.perform(get("/api/agent/briefings/news/latest")
                        .header(BriefingTokenFilter.TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(bodyOf(result)).contains("오늘의 뉴스").contains("첫 소식입니다.");
    }

    @Test
    void readEndpointsAlsoRequireTheToken() throws Exception {
        mockMvc.perform(get("/api/agent/briefings/kinds")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/agent/briefings/news/latest")).andExpect(status().isUnauthorized());
    }

    /** A leaked briefing token must not become a way into the admin area. */
    @Test
    void theBriefingTokenDoesNotOpenTheAdminArea() throws Exception {
        mockMvc.perform(get("/admin/console").header(BriefingTokenFilter.TOKEN_HEADER, TOKEN))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/admin/console/content")
                        .header(BriefingTokenFilter.TOKEN_HEADER, TOKEN))
                .andExpect(status().is3xxRedirection());
    }

    // ------------------------------------------------------------ digest page

    @Test
    void digestPageShowsEveryBriefingOfTheDayExpandedToAnonymousVisitors() throws Exception {
        publishDay();

        String html = bodyOf(mockMvc.perform(get("/briefing")).andExpect(status().isOk()).andReturn());

        assertThat(html)
                .contains("오늘의 뉴스").contains("첫 소식입니다.")
                .contains("오늘의 채용").contains("지원 마감 임박.");
    }

    @Test
    void digestPageKeepsTheWholeDayInOneMarkdownContainerForTheMathParser() throws Exception {
        publishDay();

        String html = bodyOf(mockMvc.perform(get("/briefing")).andReturn());

        assertThat(html.split("id=\"markdown-content\"", -1)).hasSize(2);
    }

    @Test
    void digestPageNavigatesByDate() throws Exception {
        publishDay();

        String today = bodyOf(mockMvc.perform(get("/briefing").param("date", TODAY)).andReturn());
        assertThat(today).contains("오늘의 뉴스");

        MvcResult empty = mockMvc.perform(get("/briefing").param("date", "2020-01-01"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(bodyOf(empty))
                .contains("이 날짜에는 브리핑이 없습니다.")
                .doesNotContain("첫 소식입니다.");
    }

    @Test
    void digestPageRejectsAMalformedDate() throws Exception {
        mockMvc.perform(get("/briefing").param("date", "yesterday"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void digestPageHidesTheSourceMemoLinkFromAnonymousVisitors() throws Exception {
        publish("news", "# 오늘의 뉴스\n\n첫 소식입니다.\n", TOKEN);

        assertThat(bodyOf(mockMvc.perform(get("/briefing")).andReturn()))
                .doesNotContain("briefing-entry__source");
    }

    @Test
    @WithMockUser
    void digestPageShowsTheSourceMemoLinkToTheAuthor() throws Exception {
        publish("news", "# 오늘의 뉴스\n\n첫 소식입니다.\n", TOKEN);

        assertThat(bodyOf(mockMvc.perform(get("/briefing")).andReturn()))
                .contains("briefing-entry__source");
    }

    // ------------------------------------------------------ leak containment

    /**
     * The blog list renders the category tree alongside the posts, so one page covers both
     * the list and the tree. A public memo is published too, so a broken assertion shows up
     * as "found neither" rather than passing on an empty page.
     */
    @Test
    void briefingsDoNotAppearInTheAnonymousBlogListOrCategoryTree() throws Exception {
        publishDay();
        memoRepository.save(new Memo("/blog/공개글", "# 공개글\n\n누구나 읽습니다.\n"));

        String html = bodyOf(mockMvc.perform(get("/public/memos"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(html).contains("공개글");
        assertThat(html).doesNotContain("오늘의 뉴스").doesNotContain("오늘의 채용");
        assertThat(html).doesNotContain("/private/briefing").doesNotContain("briefing/news");
    }

    /** Search matches on the memo name, which is exactly where the word "briefing" lives. */
    @Test
    void briefingsDoNotAppearInAnonymousSearch() throws Exception {
        publishDay();
        memoRepository.save(new Memo("/blog/briefing-공개-메모", "# 공개 메모\n\n본문\n"));

        String html = bodyOf(mockMvc.perform(get("/public/search").param("keyword", "briefing"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(html).contains("briefing-공개-메모");
        assertThat(html).doesNotContain("/private/briefing");
    }

    @Test
    void briefingMemosAreNotReachableDirectlyByAnonymousVisitors() throws Exception {
        publish("news", "# 오늘의 뉴스\n\n첫 소식입니다.\n", TOKEN);
        Memo memo = memoRepository.findByName("/private/briefing/news/" + TODAY).orElseThrow();

        mockMvc.perform(get("/public/memos/" + memo.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sitemapListsTheDigestPageButNotTheBriefingMemos() throws Exception {
        publishDay();

        String xml = bodyOf(mockMvc.perform(get("/sitemap.xml")).andExpect(status().isOk()).andReturn());

        assertThat(xml).contains("/briefing");
        assertThat(xml).doesNotContain("/private/briefing");
    }
}
