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
        "spring.security.user.password=admin"
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
}
