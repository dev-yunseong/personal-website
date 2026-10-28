package dev.yunseong.website.manage;

import dev.yunseong.website.global.repository.LoginTokenRepository;
import dev.yunseong.website.blog.service.MemoDraftService;
import dev.yunseong.website.manage.domain.ProjectReport;
import dev.yunseong.website.manage.repository.ProjectReportRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.config.import=", "app.briefing.token=console-test-token",
        "spring.datasource.url=jdbc:h2:mem:consoletest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "s3.access-key=test", "s3.secret-key=test", "s3.bucket-name=test"
})
@AutoConfigureMockMvc
class ConsoleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ProjectReportRepository reports;
    @Autowired LoginTokenRepository tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired MemoDraftService drafts;

    private String report;

    @Test
    void draftReviewIncludesIsolatedPreviewInFullAdminLayout() throws Exception {
        var draft = drafts.submit("preview/integration", "# 미리보기\n\n| 항목 | 값 |\n| --- | --- |\n| 링크 | 확인 |\n");
        mvc.perform(get("/admin/console/drafts/" + draft.getId()).with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("초안 보기 방식")))
                .andExpect(content().string(containsString("title=\"초안 미리보기\"")))
                .andExpect(content().string(containsString("sandbox=\"\"")))
                .andExpect(content().string(containsString("&lt;table&gt;")))
                .andExpect(content().string(containsString("bootstrap.bundle.min.js")))
                .andExpect(content().string(containsString("/admin/console/drafts/" + draft.getId() + "/approval")));
    }

    @BeforeEach
    void setup() throws Exception {
        reports.deleteAll();
        tokens.removeUserTokens("admin");
        report = Files.readString(Path.of("docs/examples/project-report.json"));
    }

    @Test
    void loginPageIsCustomAndIncludesCsrfAndOptIn() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("로그인")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"remember-me\"")))
                .andExpect(content().string(not(containsString("Please sign in"))));
        mvc.perform(get("/login").with(user("admin"))).andExpect(redirectedUrl("/admin/console"));
        mvc.perform(get("/admin").with(user("admin"))).andExpect(redirectedUrl("/admin/console"));
    }

    @Test
    void loginRequiresCsrfAndValidPasswordAndDoesNotPersistUnlessSelected() throws Exception {
        mvc.perform(post("/login").param("username", "admin").param("password", "admin"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "wrong")
                        .param("remember-me", "true"))
                .andExpect(redirectedUrl("/login?error"));
        assertThat(jdbc.queryForObject("select count(*) from persistent_logins", Long.class)).isZero();
        mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "admin"))
                .andExpect(redirectedUrl("/admin/console"))
                .andExpect(cookie().doesNotExist("remember-me"));
    }

    @Test
    void loginPreservesOriginallyRequestedPrivatePage() throws Exception {
        var requested = mvc.perform(get("/admin/miniapps")).andReturn();
        mvc.perform(post("/login").session((MockHttpSession) requested.getRequest().getSession())
                        .with(csrf()).param("username", "admin").param("password", "admin"))
                .andExpect(redirectedUrl("http://localhost/admin/miniapps?continue"));
    }

    @Test
    void persistentLoginRestoresSessionRotatesTokenAndLogoutRevokesIt() throws Exception {
        MvcResult login = persistentLogin();
        Cookie original = login.getResponse().getCookie("remember-me");
        assertThat(original).isNotNull();
        assertThat(original.isHttpOnly()).isTrue();
        assertThat(original.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(original.getSecure()).isTrue();
        assertThat(original.getMaxAge()).isEqualTo(30 * 24 * 60 * 60);
        MvcResult restored = mvc.perform(get("/admin/console").secure(true).cookie(original))
                .andExpect(status().isOk()).andReturn();
        Cookie rotated = restored.getResponse().getCookie("remember-me");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());
        mvc.perform(post("/logout").secure(true).cookie(rotated)
                        .session((MockHttpSession) restored.getRequest().getSession()).with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andExpect(cookie().maxAge("remember-me", 0));
        assertThat(jdbc.queryForObject("select count(*) from persistent_logins", Long.class)).isZero();
        mvc.perform(get("/admin/console").cookie(rotated)).andExpect(status().is3xxRedirection());
    }

    @Test
    void logoutRevokesPersistentTokenEvenWhenSessionHasExpired() throws Exception {
        Cookie persistent = persistentLogin().getResponse().getCookie("remember-me");
        mvc.perform(post("/logout").secure(true).cookie(persistent).with(csrf()))
                .andExpect(redirectedUrl("/"));
        assertThat(jdbc.queryForObject("select count(*) from persistent_logins", Long.class)).isZero();
        mvc.perform(get("/admin/console").cookie(persistent)).andExpect(status().is3xxRedirection());
    }

    @Test
    void expiredAndReplayedTokensCannotRestoreLogin() throws Exception {
        Cookie expired = persistentLogin().getResponse().getCookie("remember-me");
        String series = java.net.URLDecoder.decode(new String(Base64.getDecoder().decode(expired.getValue()), StandardCharsets.UTF_8).split(":")[0], StandardCharsets.UTF_8);
        PersistentRememberMeToken stored = tokens.getTokenForSeries(series);
        tokens.updateToken(series, stored.getTokenValue(), Date.from(Instant.now().minus(31, ChronoUnit.DAYS)));
        mvc.perform(get("/admin/console").cookie(expired)).andExpect(status().is3xxRedirection());

        Cookie original = persistentLogin().getResponse().getCookie("remember-me");
        mvc.perform(get("/admin/console").cookie(original)).andExpect(status().isOk());
        mvc.perform(get("/admin/console").cookie(original))
                .andExpect(status().is3xxRedirection()).andExpect(cookie().maxAge("remember-me", 0));
        assertThat(jdbc.queryForObject("select count(*) from persistent_logins", Long.class)).isZero();
    }

    @Test
    void consoleUsesAdminNavigationAndKeepsTrafficAndOldBookmarks() throws Exception {
        mvc.perform(get("/admin/console").with(user("admin")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("등록된 프로젝트가 없습니다.")))
                .andExpect(content().string(containsString("aria-label=\"관리자 메뉴\"")));
        mvc.perform(get("/admin/console/traffic").with(user("admin")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"consoleTabs\"")));
        mvc.perform(get("/admin/console").param("tab", "history").param("days", "30").with(user("admin")))
                .andExpect(forwardedUrl("/admin/console/traffic"));
        mvc.perform(get("/admin/console/projects/unknown").with(user("admin"))).andExpect(status().isNotFound());
    }

    @Test
    void projectIntakeRequiresMachineTokenAndCannotGrantAdminAccess() throws Exception {
        mvc.perform(put("/api/agent/projects/website").contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/agent/projects/website").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/console").header("X-Briefing-Token", "console-test-token"))
                .andExpect(status().is3xxRedirection());
        publish("website", report).andExpect(status().isNoContent())
                .andExpect(header().string("Location", "/admin/console/projects/website"));
    }

    @Test
    void reportsReplaceByProjectAndRenderAllWidgetTypesAsEscapedText() throws Exception {
        publish("website", report).andExpect(status().isNoContent());
        String updated = report.replace("Personal website", "<script>alert(1)</script>").replace("RUNNING", "BLOCKED");
        publish("website", updated).andExpect(status().isNoContent());
        assertThat(reports.count()).isEqualTo(1);
        mvc.perform(get("/admin/console").with(user("admin")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
        mvc.perform(get("/admin/console/projects/website").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("widget-metric")))
                .andExpect(content().string(containsString("data-project-chart")))
                .andExpect(content().string(containsString("완료한 작업 추이")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    void staleReportsAreFlaggedWithoutChangingReportedStatus() throws Exception {
        reports.save(new ProjectReport("old", report, Instant.now().minus(3, ChronoUnit.HOURS)));
        mvc.perform(get("/admin/console").with(user("admin")))
                .andExpect(content().string(containsString("갱신 지연")));
        mvc.perform(get("/admin/console/projects/old").with(user("admin")))
                .andExpect(content().string(containsString("위 상태는 마지막 보고 당시의 상태입니다.")))
                .andExpect(content().string(containsString("진행 중")));
    }

    @Test
    void rejectsMalformedOversizedAndInvalidReportsWithoutOverwriting() throws Exception {
        publish("website", report).andExpect(status().isNoContent());
        for (String invalid : new String[]{"null", "{}", "{", " ".repeat(65_537),
                report.replace("RUNNING", "OTHER"), report.replace("120", "0"),
                report.replace("\"value\": 1}", "\"value\": 1e300}"),
                report.replace("[\"작업\", \"상태\"]", "[\"작업\"]")}) {
            publish("website", invalid).andExpect(status().isBadRequest());
        }
        publish("-invalid", report).andExpect(status().isBadRequest());
        assertThat(reports.findById("website").orElseThrow().getPayload()).contains("Personal website");
    }

    private org.springframework.test.web.servlet.ResultActions publish(String slug, String payload) throws Exception {
        return mvc.perform(put("/api/agent/projects/" + slug).header("X-Briefing-Token", "console-test-token")
                .contentType(MediaType.APPLICATION_JSON).content(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private MvcResult persistentLogin() throws Exception {
        return mvc.perform(post("/login").secure(true).with(csrf()).param("username", "admin")
                        .param("password", "admin").param("remember-me", "true"))
                .andExpect(redirectedUrl("/admin/console")).andReturn();
    }
}
