package dev.yunseong.website.briefing.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    @Mock
    private MemoService memoService;

    @InjectMocks
    private BriefingService briefingService;

    private static Memo briefing(long id, String kind, String date) {
        return new Memo(id, "/private/briefing/" + kind + "/" + date, "content",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void publish_StoresBriefingAsPrivateMemoNamedByKindAndDate() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        briefingService.publish("news", DATE, "# 오늘의 뉴스\n\n본문");

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(memoService).upsertMemo(nameCaptor.capture(), anyString());
        assertThat(nameCaptor.getValue()).isEqualTo("/private/briefing/news/2026-08-12");
    }

    @Test
    void publish_KeepsBriefingsUnderThePrivatePrefix() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        Memo memo = briefingService.publish("jobs", DATE, "# 채용\n\n본문");

        assertThat(memo.getName()).startsWith(Memo.PRIVATE_PREFIX);
        assertThat(memo.isPrivate()).isTrue();
    }

    @Test
    void publish_WrapsBodyWithTitleAndMetaLine() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        Memo memo = briefingService.publish("news", DATE, "# 오늘의 뉴스\n\n첫 소식\n");

        assertThat(memo.getContent())
                .startsWith("# 오늘의 뉴스\n")
                .contains("`news` · 2026-08-12 · 에이전트 작성")
                .contains("첫 소식");
    }

    @Test
    void publish_DoesNotRepeatTheTitleHeadingInTheBody() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        Memo memo = briefingService.publish("news", DATE, "# 오늘의 뉴스\n\n첫 소식\n");

        assertThat(memo.getContent().split("# 오늘의 뉴스", -1)).hasSize(2);
    }

    @Test
    void publish_FallsBackToKindAndDateWhenBodyHasNoHeading() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        Memo memo = briefingService.publish("news", DATE, "제목 없는 본문");

        assertThat(memo.getContent()).startsWith("# news 2026-08-12\n");
        assertThat(memo.getContent()).contains("제목 없는 본문");
    }

    @Test
    void publish_UsesTodayWhenDateIsOmitted() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        Memo memo = briefingService.publish("news", null, "# 제목\n\n본문");

        assertThat(memo.getName())
                .isEqualTo("/private/briefing/news/" + LocalDate.now());
    }

    @Test
    void publish_UpsertsSoRepublishingTheSameDayReplacesRatherThanAppends() {
        Memo existing = briefing(7L, "news", "2026-08-12");
        when(memoService.upsertMemo(eq("/private/briefing/news/2026-08-12"), anyString()))
                .thenReturn(existing);

        Memo first = briefingService.publish("news", DATE, "# 제목\n\n1차");
        Memo second = briefingService.publish("news", DATE, "# 제목\n\n2차");

        assertThat(first).isSameAs(second);
        verify(memoService, never()).saveMemo(anyString(), anyString());
    }

    @Test
    void publish_NormalizesKindCaseAndSurroundingSpace() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        Memo memo = briefingService.publish("  News  ", DATE, "# 제목\n\n본문");

        assertThat(memo.getName()).isEqualTo("/private/briefing/news/2026-08-12");
    }

    /**
     * The kind lands in the memo name unescaped, so anything that could climb out of
     * the briefing prefix has to be refused before it is concatenated.
     */
    @Test
    void publish_RejectsKindThatCouldEscapeTheBriefingPrefix() {
        List<String> hostileKinds = List.of(
                "../curator", "news/../../curator", "news/extra", ".", "..",
                "news.md", "NEWS!", "한국어", "-news", "", "   ",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        for (String kind : hostileKinds) {
            assertThatThrownBy(() -> briefingService.publish(kind, DATE, "# 제목\n\n본문"))
                    .as("kind %s", kind)
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(memoService, never()).upsertMemo(anyString(), anyString());
    }

    @Test
    void publish_RejectsEmptyBody() {
        assertThatThrownBy(() -> briefingService.publish("news", DATE, "   \n  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        verify(memoService, never()).upsertMemo(anyString(), anyString());
    }

    @Test
    void publish_RejectsBodyOverTheLengthLimit() {
        String tooLong = "#".repeat(20_001);

        assertThatThrownBy(() -> briefingService.publish("news", DATE, tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too long");
        verify(memoService, never()).upsertMemo(anyString(), anyString());
    }

    @Test
    void findLatest_ReturnsTheBriefingWithTheNewestDate() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/news/"), anyInt()))
                .thenReturn(List.of(
                        briefing(1L, "news", "2026-08-10"),
                        briefing(3L, "news", "2026-08-12"),
                        briefing(2L, "news", "2026-08-11")));

        Optional<Memo> latest = briefingService.findLatest("news");

        assertThat(latest).map(Memo::getId).contains(3L);
    }

    @Test
    void findLatest_ReturnsEmptyForAKindThatWasNeverPublished() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/unknown/"), anyInt()))
                .thenReturn(List.of());

        assertThat(briefingService.findLatest("unknown")).isEmpty();
    }

    @Test
    void listKinds_DerivesKindsFromStoredNamesWithoutARegistry() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(
                        briefing(1L, "news", "2026-08-12"),
                        briefing(2L, "jobs", "2026-08-12"),
                        briefing(3L, "news", "2026-08-11")));

        assertThat(briefingService.listKinds()).containsExactly("jobs", "news");
    }

    /**
     * A kind exists the moment something is published under it — no code here
     * enumerates the known kinds, so a brand new one shows up on its own.
     */
    @Test
    void listKinds_PicksUpAKindNobodyRegistered() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(briefing(1L, "weather-alerts", "2026-08-12")));

        assertThat(briefingService.listKinds()).containsExactly("weather-alerts");
    }

    @Test
    void listKinds_IgnoresNamesThatAreNotShapedLikeABriefing() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(
                        briefing(1L, "news", "2026-08-12"),
                        new Memo(2L, "/private/briefing/orphan", "x", null, null),
                        new Memo(3L, "/private/briefing/news/nested/deep", "x", null, null)));

        assertThat(briefingService.listKinds()).containsExactly("news");
    }

    @Test
    void recentDates_ReturnsDistinctDatesNewestFirst() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(
                        briefing(1L, "news", "2026-08-10"),
                        briefing(2L, "jobs", "2026-08-12"),
                        briefing(3L, "news", "2026-08-12")));

        assertThat(briefingService.recentDates(10))
                .containsExactly(LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 10));
    }

    @Test
    void recentDates_HonoursTheLimit() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(
                        briefing(1L, "news", "2026-08-10"),
                        briefing(2L, "news", "2026-08-11"),
                        briefing(3L, "news", "2026-08-12")));

        assertThat(briefingService.recentDates(2))
                .containsExactly(LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 11));
    }

    @Test
    void findByDate_ReturnsEveryKindPublishedThatDayOrderedByKind() {
        Memo news = briefing(1L, "news", "2026-08-12");
        Memo jobs = briefing(2L, "jobs", "2026-08-12");
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(news, jobs));
        when(memoService.findMemoWithoutVisibilityFilter("/private/briefing/jobs/2026-08-12"))
                .thenReturn(Optional.of(jobs));
        when(memoService.findMemoWithoutVisibilityFilter("/private/briefing/news/2026-08-12"))
                .thenReturn(Optional.of(news));

        assertThat(briefingService.findByDate(DATE)).containsExactly(jobs, news);
    }

    @Test
    void findByDate_SkipsKindsWithNothingPublishedThatDay() {
        Memo news = briefing(1L, "news", "2026-08-12");
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(news, briefing(2L, "jobs", "2026-08-01")));
        when(memoService.findMemoWithoutVisibilityFilter("/private/briefing/jobs/2026-08-12"))
                .thenReturn(Optional.empty());
        when(memoService.findMemoWithoutVisibilityFilter("/private/briefing/news/2026-08-12"))
                .thenReturn(Optional.of(news));

        assertThat(briefingService.findByDate(DATE)).containsExactly(news);
    }

    /**
     * The digest page renders whatever this returns without the visibility filter, so a
     * lookup must never reach a memo outside the briefing prefix.
     */
    @Test
    void findByDate_OnlyEverLooksUpNamesUnderTheBriefingPrefix() {
        when(memoService.getMemosByNamePrefix(eq("/private/briefing/"), anyInt()))
                .thenReturn(List.of(briefing(1L, "news", "2026-08-12")));
        lenient().when(memoService.findMemoWithoutVisibilityFilter(anyString()))
                .thenReturn(Optional.empty());

        briefingService.findByDate(DATE);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(memoService).findMemoWithoutVisibilityFilter(nameCaptor.capture());
        assertThat(nameCaptor.getAllValues())
                .allSatisfy(name -> assertThat(name).startsWith(BriefingService.BRIEFING_ROOT + "/"));
    }

    @Test
    void findByKindAndDate_ReadsTheMemoForThatKindAndDate() {
        Memo news = briefing(1L, "news", "2026-08-12");
        when(memoService.findMemoWithoutVisibilityFilter("/private/briefing/news/2026-08-12"))
                .thenReturn(Optional.of(news));

        assertThat(briefingService.findByKindAndDate("news", DATE)).contains(news);
    }

    @Test
    void kindOfAndDateOf_ReadTheStoredName() {
        Memo memo = briefing(1L, "news", "2026-08-12");

        assertThat(briefingService.kindOf(memo)).contains("news");
        assertThat(briefingService.dateOf(memo)).contains(DATE);
    }

    @Test
    void kindOf_IsEmptyForAMemoOutsideTheBriefingPrefix() {
        Memo memo = new Memo(1L, "/private/curator/AGENTS.md", "x", null, null);

        assertThat(briefingService.kindOf(memo)).isEmpty();
        assertThat(briefingService.dateOf(memo)).isEmpty();
    }

    @Test
    void parseRequiredDate_RejectsAnythingThatIsNotAnIsoDate() {
        assertThat(BriefingService.parseRequiredDate("2026-08-12")).isEqualTo(DATE);
        assertThatThrownBy(() -> BriefingService.parseRequiredDate("2026/08/12"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BriefingService.parseRequiredDate("yesterday"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findLatest_RejectsAnInvalidKindRatherThanQuerying() {
        assertThatThrownBy(() -> briefingService.findLatest("../curator"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(memoService, never()).getMemosByNamePrefix(anyString(), anyInt());
    }

    @Test
    void publishedContentRendersAsMarkdown() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        Memo memo = briefingService.publish("news", DATE, "# 오늘의 뉴스\n\n- 첫 소식\n");

        assertThat(memo.getHtml())
                .contains("<h1>오늘의 뉴스</h1>")
                .contains("<li>첫 소식</li>")
                .contains("<code>news</code>");
    }

    @Test
    void publish_DoesNotTouchAnyOtherMemoApi() {
        when(memoService.upsertMemo(anyString(), anyString()))
                .thenAnswer(invocation -> new Memo(invocation.getArgument(0), invocation.getArgument(1)));

        briefingService.publish("news", DATE, "# 제목\n\n본문");

        verify(memoService, never()).updateMemo(any(Long.class), anyString(), anyString());
        verify(memoService, never()).moveMemo(anyString(), anyString());
    }
}
