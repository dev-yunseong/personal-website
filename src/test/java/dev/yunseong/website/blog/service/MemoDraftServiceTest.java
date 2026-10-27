package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.config.import=", "spring.profiles.include=", "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@Import(MemoDraftService.class)
class MemoDraftServiceTest {
    @Autowired MemoDraftService drafts;
    @Autowired MemoRepository repository;

    @Test
    void newDraftStaysPrivateUntilApproval() {
        Memo draft = drafts.submit("notes/new", "# 한글\n\nbody\n");
        assertThat(draft.getName()).isEqualTo("/private/draft/notes/new");
        assertThat(draft.isPrivate()).isTrue();
        assertThat(repository.findPublicById(draft.getId())).isEmpty();
        assertThat(repository.findByName("/notes/new")).isEmpty();
        var review = drafts.review(draft.getId());
        long publishedId = drafts.approve(draft.getId(), review.revision());
        assertThat(repository.findById(publishedId).orElseThrow().getContent()).isEqualTo("# 한글\n\nbody\n");
        assertThat(repository.findById(draft.getId())).isEmpty();
    }

    @Test
    void replacesExistingMemoWithoutChangingIdentity() {
        Memo current = repository.saveAndFlush(new Memo("/notes/existing", "old"));
        Memo draft = drafts.submit("/notes/existing", "new");
        assertThat(current.getContent()).isEqualTo("old");
        assertThat(drafts.review(draft.getId()).lines()).extracting(MemoLineDiff.Line::marker).containsExactly("-", "+");
        assertThat(drafts.approve(draft.getId(), drafts.review(draft.getId()).revision())).isEqualTo(current.getId());
        assertThat(current.getContent()).isEqualTo("new");
    }

    @Test
    void resubmissionInvalidatesPreviouslyReviewedDraft() {
        Memo draft = drafts.submit("notes/a", "first");
        String revision = drafts.review(draft.getId()).revision();
        assertThat(drafts.submit("notes/a", "second").getId()).isEqualTo(draft.getId());
        assertConflict(draft.getId(), revision);
        assertThat(repository.findByName("/notes/a")).isEmpty();
    }

    @Test
    void changedOrNewlyCreatedTargetInvalidatesReview() {
        Memo draft = drafts.submit("notes/a", "proposal");
        String revision = drafts.review(draft.getId()).revision();
        Memo target = repository.saveAndFlush(new Memo("/notes/a", "created elsewhere"));
        assertConflict(draft.getId(), revision);
        revision = drafts.review(draft.getId()).revision();
        target.setContent("edited elsewhere");
        repository.flush();
        assertConflict(draft.getId(), revision);
        assertThat(target.getContent()).isEqualTo("edited elsewhere");
        assertThat(repository.findById(draft.getId())).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/private/profile", "/private/draft/x", "/", "a//b", "a/../b", "a/", "a\\b"})
    void rejectsInvalidOrPrivateTargets(String target) {
        assertThatThrownBy(() -> drafts.submit(target, "body")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void cannotApproveOrdinaryMemoOrOversizedDraft() {
        Memo memo = repository.saveAndFlush(new Memo("/notes/a", "original"));
        assertThatThrownBy(() -> drafts.approve(memo.getId(), "anything")).isInstanceOf(MemoNotFoundException.class);
        assertThatThrownBy(() -> drafts.submit("notes/b", "x".repeat(100_001))).isInstanceOf(ResponseStatusException.class);
    }

    private void assertConflict(long id, String revision) {
        assertThatThrownBy(() -> drafts.approve(id, revision)).isInstanceOfSatisfying(
                ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void renamePreservesDraftIdentityBodyAndPublishedSource() {
        Memo published = repository.saveAndFlush(new Memo("/notes/old", "published"));
        Memo draft = drafts.submit("notes/old", "# 제목\n\n본문\n");
        String revision = drafts.review(draft.getId()).revision();
        Memo renamed = drafts.rename("/notes/old", "framework/new");
        assertThat(renamed.getId()).isEqualTo(draft.getId());
        assertThat(renamed.getContent()).isEqualTo("# 제목\n\n본문\n");
        assertThat(renamed.getName()).isEqualTo("/private/draft/framework/new");
        assertThat(repository.findByName("/private/draft/notes/old")).isEmpty();
        assertThat(repository.findPublicById(draft.getId())).isEmpty();
        assertThat(repository.findByName("/framework/new")).isEmpty();
        assertThat(published.getName()).isEqualTo("/notes/old");
        assertThat(published.getContent()).isEqualTo("published");
        assertConflict(draft.getId(), revision);
    }

    @Test
    void renameToSameNormalizedPathIsNoOp() {
        Memo draft = drafts.submit("notes/a", "body");
        String revision = drafts.review(draft.getId()).revision();
        assertThat(drafts.rename("notes/a", "/notes/a").getId()).isEqualTo(draft.getId());
        assertThat(drafts.review(draft.getId()).revision()).isEqualTo(revision);
    }

    @Test
    void renameRefusesOccupiedDestinationsWithoutChangingEitherMemo() {
        Memo source = drafts.submit("notes/a", "source");
        Memo pending = drafts.submit("notes/b", "pending");
        Memo published = repository.saveAndFlush(new Memo("/notes/c", "published"));
        for (String target : new String[]{"notes/b", "notes/c"}) {
            assertThatThrownBy(() -> drafts.rename("notes/a", target)).isInstanceOfSatisfying(
                    ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        }
        assertThat(source.getName()).isEqualTo("/private/draft/notes/a");
        assertThat(pending.getContent()).isEqualTo("pending");
        assertThat(published.getContent()).isEqualTo("published");
    }

    @Test
    void renameCannotMoveOrdinaryMemoOrMissingDraft() {
        Memo published = repository.saveAndFlush(new Memo("/notes/a", "published"));
        assertThatThrownBy(() -> drafts.rename("notes/a", "notes/b")).isInstanceOf(MemoNotFoundException.class);
        assertThatThrownBy(() -> drafts.rename("missing", "notes/b")).isInstanceOf(MemoNotFoundException.class);
        assertThat(published.getName()).isEqualTo("/notes/a");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/private/profile", "/private/draft/x", "/", "a//b", "a/../b", "a/", "a\\b", "a\nb", ""})
    void renameValidatesBothPaths(String path) {
        Memo draft = drafts.submit("notes/a", "body");
        assertThatThrownBy(() -> drafts.rename(path, "notes/b")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> drafts.rename("notes/a", path)).isInstanceOf(ResponseStatusException.class);
        assertThat(draft.getName()).isEqualTo("/private/draft/notes/a");
    }
}
