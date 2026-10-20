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
import software.amazon.awssdk.services.s3.S3Client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "s3.endpoint=http://localhost:9000",
        "s3.region=us-east-1",
        "s3.access-key=test",
        "s3.secret-key=test",
        "s3.bucket-name=test-bucket",
        "spring.datasource.url=jdbc:h2:mem:memopathdb;MODE=PostgreSQL"
})
class PublicMemoPathIntegrationTest {

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

    @ParameterizedTest
    @ValueSource(strings = {"/notes/nested/example", "/notes/한글 메모", "/notes/a+b"})
    void anonymousUserCanReadPublicMemoByExactPath(String name) throws Exception {
        Memo memo = memoRepository.saveAndFlush(new Memo(name, "public memo body"));

        mockMvc.perform(get("/public/blog{name}", name))
                .andExpect(status().isOk())
                .andExpect(view().name("memo/view"))
                .andExpect(model().attribute("memo", hasProperty("id", is(memo.getId()))))
                .andExpect(content().string(containsString("public memo body")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/private/draft", "/private/deleted/old", "/private-notes/secret"})
    void anonymousUserCannotReadPrivateMemoByPath(String name) throws Exception {
        memoRepository.saveAndFlush(new Memo(name, "secret draft body"));

        mockMvc.perform(get("/public/blog{name}", name))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("secret draft body"))));
    }

    @Test
    @WithMockUser
    void authenticatedUserCanReadPrivateDraftByPath() throws Exception {
        Memo memo = memoRepository.saveAndFlush(new Memo("/private/draft", "secret draft body"));

        mockMvc.perform(get("/public/blog/private/draft"))
                .andExpect(status().isOk())
                .andExpect(view().name("memo/view"))
                .andExpect(model().attribute("memo", hasProperty("id", is(memo.getId()))))
                .andExpect(content().string(containsString("secret draft body")));
    }

    @Test
    void missingMemoFallsBackToEmptyCategory() throws Exception {
        mockMvc.perform(get("/public/blog/notes/missing"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog"))
                .andExpect(model().attribute("selectedCategory", "/notes/missing"))
                .andExpect(model().attribute("memos", hasProperty("totalElements", is(0L))));
    }

    @Test
    @WithMockUser
    void authenticatedMissingMemoFallsBackToEmptyCategory() throws Exception {
        mockMvc.perform(get("/public/blog/private/missing"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog"))
                .andExpect(model().attribute("selectedCategory", "/private/missing"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/notes/java", "/notes/java/"})
    void pathWithoutExactMemoShowsCategory(String path) throws Exception {
        memoRepository.saveAndFlush(new Memo("/notes/java/first", "category child body"));
        memoRepository.saveAndFlush(new Memo("/notes/javascript/other", "unrelated body"));

        mockMvc.perform(get("/public/blog{path}", path))
                .andExpect(status().isOk())
                .andExpect(view().name("blog"))
                .andExpect(model().attribute("selectedCategory", "/notes/java"))
                .andExpect(model().attribute("memos", hasProperty("totalElements", is(1L))));
    }

    @Test
    void exactMemoTakesPrecedenceOverCategory() throws Exception {
        Memo parent = memoRepository.saveAndFlush(new Memo("/notes/java", "parent memo body"));
        memoRepository.saveAndFlush(new Memo("/notes/java/child", "child memo body"));

        mockMvc.perform(get("/public/blog/notes/java"))
                .andExpect(status().isOk())
                .andExpect(view().name("memo/view"))
                .andExpect(model().attribute("memo", hasProperty("id", is(parent.getId()))));
    }

    @Test
    @WithMockUser
    void authenticatedUserCanBrowsePrivateCategory() throws Exception {
        memoRepository.saveAndFlush(new Memo("/private/drafts/first", "private category body"));

        mockMvc.perform(get("/public/blog/private/drafts"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog"))
                .andExpect(model().attribute("selectedCategory", "/private/drafts"))
                .andExpect(model().attribute("memos", hasProperty("totalElements", is(1L))));
    }

    @Test
    void anonymousUserCannotBrowsePrivateCategory() throws Exception {
        memoRepository.saveAndFlush(new Memo("/private/drafts/first", "private category body"));

        mockMvc.perform(get("/public/blog/private/drafts"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("private category body"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/public/blog", "/public/blog/"})
    void rootPathShowsAllPublicMemos(String path) throws Exception {
        memoRepository.saveAndFlush(new Memo("/notes/public", "public body"));
        memoRepository.saveAndFlush(new Memo("/private/secret", "private body"));

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name("blog"))
                .andExpect(model().attribute("memos", hasProperty("totalElements", is(1L))));
    }

    @Test
    void categoryPaginationKeepsTheSelectedPath() throws Exception {
        memoRepository.saveAndFlush(new Memo("/notes/java/first", "first body"));
        memoRepository.saveAndFlush(new Memo("/notes/java/second", "second body"));

        mockMvc.perform(get("/public/blog/notes/java").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedCategory", "/notes/java"))
                .andExpect(model().attribute("memos", hasProperty("number", is(1))))
                .andExpect(model().attribute("memos", hasProperty("totalElements", is(2L))));
    }

    @Test
    void encodedPercentRemainsRejectedByTheSecurityFirewall() throws Exception {
        mockMvc.perform(get("/public/blog{name}", "/notes/100% complete"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void numericMemoUrlStillRendersTheSameMemo() throws Exception {
        Memo memo = memoRepository.saveAndFlush(new Memo("/notes/existing", "existing memo body"));

        mockMvc.perform(get("/public/memos/{id}", memo.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("memo/view"))
                .andExpect(model().attribute("memo", hasProperty("id", is(memo.getId()))));
    }

    @Test
    void numericPrivateMemoUrlRemainsHidden() throws Exception {
        Memo memo = memoRepository.saveAndFlush(new Memo("/private/draft", "secret draft body"));

        mockMvc.perform(get("/public/memos/{id}", memo.getId()))
                .andExpect(status().isNotFound());
    }
}
