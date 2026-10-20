package dev.yunseong.website.blog.controller;

import dev.yunseong.website.ai.tool.BlogTools;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import dev.yunseong.website.blog.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "s3.endpoint=http://localhost:9000",
        "s3.region=us-east-1",
        "s3.access-key=test",
        "s3.secret-key=test",
        "s3.bucket-name=test-bucket",
        "spring.datasource.url=jdbc:h2:mem:publicmemoapidb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.user.name=admin",
        "spring.security.user.password=admin",
        "app.draft.token=navigation-test-token"
})
class PublicMemoApiIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearMemos() {
        memoRepository.deleteAll();
    }

    @Test
    void listReturnsPublicMemoMetadataWithoutContent() throws Exception {
        Memo publicMemo = memoRepository.saveAndFlush(
                new Memo("/notes/공개", "# 공개\n\n비밀 아닌 본문\n"));

        mockMvc.perform(get("/api/public/memos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(publicMemo.getId()))
                .andExpect(jsonPath("$.items[0].name").value("/notes/공개"))
                .andExpect(jsonPath("$.items[0].updatedAt").isString())
                .andExpect(jsonPath("$.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(100))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void updatedAfterIsStrictlyExclusive() throws Exception {
        Memo atBoundary = memoRepository.saveAndFlush(new Memo("/notes/boundary", "boundary"));
        Memo afterBoundary = memoRepository.saveAndFlush(new Memo("/notes/after", "after"));
        setUpdatedAt(atBoundary, LocalDateTime.parse("2026-08-15T10:00:00"));
        setUpdatedAt(afterBoundary, LocalDateTime.parse("2026-08-15T10:00:01"));

        mockMvc.perform(get("/api/public/memos")
                        .param("updatedAfter", "2026-08-15T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(afterBoundary.getId()));
    }

    @Test
    void negativePageIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/public/memos").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void limitOutsideAcceptedRangeIsRejected(int limit) throws Exception {
        mockMvc.perform(get("/api/public/memos").param("limit", Integer.toString(limit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readReturnsPublicMarkdownByteForByteAsUtf8() throws Exception {
        String markdown = "# 공개 메모\n\n```java\nSystem.out.println(\"안녕\");\n```\n\n";
        Memo memo = memoRepository.saveAndFlush(new Memo("/notes/markdown", markdown));

        mockMvc.perform(get("/api/public/memos/{memoId}/content", memo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/markdown;charset=UTF-8"))
                .andExpect(content().bytes(markdown.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void anonymousListExcludesPrivateAndDeletedMemos() throws Exception {
        assertListExcludesPrivateAndDeletedMemos();
    }

    @Test
    @WithMockUser
    void authenticatedListStillExcludesPrivateAndDeletedMemos() throws Exception {
        assertListExcludesPrivateAndDeletedMemos();
    }

    private void assertListExcludesPrivateAndDeletedMemos() throws Exception {
        Memo publicMemo = memoRepository.saveAndFlush(new Memo("/notes/public", "public"));
        memoRepository.saveAndFlush(new Memo("/private/secret", "secret"));
        memoRepository.saveAndFlush(new Memo("/private/deleted/old", "deleted"));

        mockMvc.perform(get("/api/public/memos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(publicMemo.getId()));
        mockMvc.perform(get("/api/public/memos")
                        .param("updatedAfter", "2000-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(publicMemo.getId()));
    }

    @Test
    void anonymousReadHidesPrivateDeletedAndMissingMemos() throws Exception {
        assertPrivateDeletedAndMissingReadsAreNotFound();
    }

    @Test
    @WithMockUser
    void authenticatedReadStillHidesPrivateDeletedAndMissingMemos() throws Exception {
        assertPrivateDeletedAndMissingReadsAreNotFound();
    }

    private void assertPrivateDeletedAndMissingReadsAreNotFound() throws Exception {
        Memo privateMemo = memoRepository.saveAndFlush(new Memo("/private/secret", "secret"));
        Memo deletedMemo = memoRepository.saveAndFlush(new Memo("/private/deleted/old", "deleted"));

        mockMvc.perform(get("/api/public/memos/{memoId}/content", privateMemo.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/memos/{memoId}/content", deletedMemo.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/memos/{memoId}/content", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void sameTimestampUsesIdAsStablePaginationTieBreaker() throws Exception {
        Memo first = memoRepository.saveAndFlush(new Memo("/notes/first", "first"));
        Memo second = memoRepository.saveAndFlush(new Memo("/notes/second", "second"));
        LocalDateTime sameTime = LocalDateTime.parse("2026-08-15T11:00:00");
        setUpdatedAt(first, sameTime);
        setUpdatedAt(second, sameTime);

        mockMvc.perform(get("/api/public/memos").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(first.getId()))
                .andExpect(jsonPath("$.hasNext").value(true));
        mockMvc.perform(get("/api/public/memos").param("limit", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(second.getId()))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void malformedUpdatedAfterIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/public/memos").param("updatedAfter", "2026-08-15T11:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    private void setUpdatedAt(Memo memo, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE memos SET updated_at = ? WHERE id = ?",
                Timestamp.valueOf(updatedAt),
                memo.getId());
    }

    @Test
    void publicTreeShowsNestedMemosButNeverDraftsOrPrivatePaths() throws Exception {
        Memo memo = memoRepository.saveAndFlush(new Memo("/notes/spring/한글", "body must not appear"));
        memoRepository.saveAndFlush(new Memo("/private/draft/notes/new", "unpublished"));
        memoRepository.saveAndFlush(new Memo("/private/secret", "secret"));
        mockMvc.perform(get("/api/public/memos/tree"))
                .andExpect(status().isOk()).andExpect(content().string(
                        "/\n└── notes/\n    └── spring/\n        └── 한글 [id=" + memo.getId() + "]\n"));
    }

    @Test
    @WithMockUser
    void publicTreeDoesNotShowDraftMarkersEvenWithAdminSession() throws Exception {
        Memo memo = memoRepository.saveAndFlush(new Memo("/notes/a", "published"));
        memoRepository.saveAndFlush(new Memo("/private/draft/notes/a", "draft"));
        mockMvc.perform(get("/api/public/memos/tree").param("path", "notes/"))
                .andExpect(status().isOk()).andExpect(content().string("/notes/\n└── a [id=" + memo.getId() + "]\n"));
    }

    @Test
    void authenticatedTreeAttachesDraftsToTheirTargets() throws Exception {
        Memo memo = memoRepository.saveAndFlush(new Memo("/notes/a", "published"));
        memoRepository.saveAndFlush(new Memo("/private/draft/notes/a", "replacement"));
        memoRepository.saveAndFlush(new Memo("/private/draft/notes/new", "new"));
        memoRepository.saveAndFlush(new Memo("/notebook/outside", "outside"));
        memoRepository.saveAndFlush(new Memo("/private/secret", "secret"));
        mockMvc.perform(get("/api/agent/drafts/tree").header("X-Draft-Token", "navigation-test-token").param("path", "/notes"))
                .andExpect(status().isOk()).andExpect(content().string(
                        "/notes/\n├── a [id=" + memo.getId() + "] [draft]\n└── new [draft only]\n"));
    }

    @Test
    void categoryIsLiteralAndListsDirectChildrenWithPaginationAndTimeFilter() throws Exception {
        Memo first = memoRepository.saveAndFlush(new Memo("/a_%./first", "one"));
        Memo second = memoRepository.saveAndFlush(new Memo("/a_%./second", "two"));
        memoRepository.saveAndFlush(new Memo("/a_%./nested/hidden", "nested"));
        memoRepository.saveAndFlush(new Memo("/aZZZ/other", "wildcard lookalike"));
        setUpdatedAt(first, LocalDateTime.parse("2026-01-01T12:00:00"));
        setUpdatedAt(second, LocalDateTime.parse("2026-01-02T12:00:00"));
        mockMvc.perform(get("/api/public/memos").param("category", "a_%./").param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(second.getId()))
                .andExpect(jsonPath("$.hasNext").value(true));
        mockMvc.perform(get("/api/public/memos").param("category", "/a_%.").param("limit", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(first.getId()))
                .andExpect(jsonPath("$.hasNext").value(false));
        mockMvc.perform(get("/api/public/memos").param("category", "/a_%.").param("updatedAfter", "2026-01-01T12:00:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(second.getId()));
    }

    @Test
    void tokenListMergesPublishedAndDraftOnlyMemosWithoutExposingStoragePaths() throws Exception {
        Memo published = memoRepository.saveAndFlush(new Memo("/notes/a", "old"));
        Memo draft = memoRepository.saveAndFlush(new Memo("/private/draft/notes/a", "new"));
        Memo pending = memoRepository.saveAndFlush(new Memo("/private/draft/notes/b", "pending"));
        setUpdatedAt(draft, LocalDateTime.parse("2099-01-02T12:00:00"));
        setUpdatedAt(pending, LocalDateTime.parse("2099-01-01T12:00:00"));
        mockMvc.perform(get("/api/agent/drafts/memos").header("X-Draft-Token", "navigation-test-token").param("category", "/notes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(published.getId()))
                .andExpect(jsonPath("$.items[0].hasDraft").value(true))
                .andExpect(jsonPath("$.items[1].name").value("/notes/b"))
                .andExpect(jsonPath("$.items[1].id").isEmpty())
                .andExpect(jsonPath("$.items[1].hasDraft").value(true));
    }

    @Test
    void draftReadUsesPublishedIdOrTargetPathAndPreservesUtf8() throws Exception {
        Memo published = memoRepository.saveAndFlush(new Memo("/notes/a", "old"));
        String body = "# 초안\n\n한글 & +\n\n";
        memoRepository.saveAndFlush(new Memo("/private/draft/notes/a", body));
        memoRepository.saveAndFlush(new Memo("/private/draft/notes/new", body));
        mockMvc.perform(get("/api/agent/drafts/content").header("X-Draft-Token", "navigation-test-token")
                        .param("memoId", published.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().bytes(body.getBytes(StandardCharsets.UTF_8)));
        mockMvc.perform(get("/api/agent/drafts/content").header("X-Draft-Token", "navigation-test-token").param("target", "notes/new"))
                .andExpect(status().isOk()).andExpect(content().bytes(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void draftNavigationRequiresTokenAndCannotReadOtherPrivateMemos() throws Exception {
        Memo privateMemo = memoRepository.saveAndFlush(new Memo("/private/secret", "secret"));
        memoRepository.saveAndFlush(new Memo("/private/draft/private/secret", "invalid private target"));
        for (String route : new String[]{"tree", "memos", "content"}) {
            mockMvc.perform(get("/api/agent/drafts/" + route)).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/agent/drafts/" + route).header("X-Draft-Token", "wrong"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/agent/drafts/content").header("X-Draft-Token", "navigation-test-token").param("target", "/private/secret"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/agent/drafts/content").header("X-Draft-Token", "navigation-test-token").param("memoId", privateMemo.getId().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/agent/drafts/content").header("X-Draft-Token", "navigation-test-token").param("target", "/notes/missing"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/agent/drafts/tree").header("X-Draft-Token", "navigation-test-token").param("path", "/private"))
                .andExpect(content().string("/private/\n"));
        mockMvc.perform(get("/api/agent/drafts/tree").header("X-Draft-Token", "navigation-test-token"))
                .andExpect(content().string("/\n"));
    }

    @Test
    void treeHandlesMemoThatIsAlsoCategoryAndEmptyScope() throws Exception {
        Memo parent = memoRepository.saveAndFlush(new Memo("/notes/a", "parent"));
        Memo child = memoRepository.saveAndFlush(new Memo("/notes/a/b", "child"));
        mockMvc.perform(get("/api/public/memos/tree").param("path", "/notes/a"))
                .andExpect(content().string("/notes/a/ [id=" + parent.getId() + "]\n└── b [id=" + child.getId() + "]\n"));
        mockMvc.perform(get("/api/public/memos/tree").param("path", "/empty"))
                .andExpect(content().string("/empty/\n"));
        mockMvc.perform(get("/api/public/memos/tree").param("path", "/notes/../private"))
                .andExpect(status().isBadRequest());
    }
}
