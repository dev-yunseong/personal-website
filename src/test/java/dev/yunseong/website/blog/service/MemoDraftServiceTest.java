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

    @Test
    void rejectionDeletesOnlyDraftAndPreservesOriginal() {
        Memo original = repository.saveAndFlush(new Memo("/notes/a", "original"));
        Memo draft = drafts.submit("notes/a", "proposal");
        drafts.reject(draft.getId(), drafts.review(draft.getId()).revision());
        repository.flush();
        assertThat(repository.findById(draft.getId())).isEmpty();
        assertThat(repository.findById(original.getId()).orElseThrow().getContent()).isEqualTo("original");
    }

    @Test
    void rejectionOfNewDraftDoesNotPublishMemo() {
        Memo draft = drafts.submit("notes/new", "proposal");
        drafts.reject(draft.getId(), drafts.review(draft.getId()).revision());
        repository.flush();
        assertThat(repository.findById(draft.getId())).isEmpty();
        assertThat(repository.findByName("/notes/new")).isEmpty();
    }

    @Test
    void rejectionRefusesChangedDraft() {
        Memo draft = drafts.submit("notes/a", "first");
        String revision = drafts.review(draft.getId()).revision();
        drafts.submit("notes/a", "second");
        assertThatThrownBy(() -> drafts.reject(draft.getId(), revision)).isInstanceOfSatisfying(
                ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThat(repository.findById(draft.getId()).orElseThrow().getContent()).isEqualTo("second");
    }

    @Test
    void rejectionRefusesOrdinaryMemoAndMissingDraft() {
        Memo memo = repository.saveAndFlush(new Memo("/notes/a", "original"));
        assertThatThrownBy(() -> drafts.reject(memo.getId(), "r")).isInstanceOf(MemoNotFoundException.class);
        assertThatThrownBy(() -> drafts.reject(-1, "r")).isInstanceOf(MemoNotFoundException.class);
        assertThat(repository.findById(memo.getId())).isPresent();
    }

    private void assertConflict(long id, String revision) {
        assertThatThrownBy(() -> drafts.approve(id, revision)).isInstanceOfSatisfying(
                ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
}
