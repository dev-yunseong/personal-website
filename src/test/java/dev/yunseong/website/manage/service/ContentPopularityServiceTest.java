package dev.yunseong.website.manage.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import dev.yunseong.website.manage.domain.CategoryTrafficStat;
import dev.yunseong.website.manage.domain.MemoTrafficStat;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.repository.ContentPopularityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentPopularityServiceTest {

    @Mock
    private ContentPopularityRepository contentPopularityRepository;

    @Mock
    private MemoRepository memoRepository;

    private ContentPopularityService contentPopularityService;

    @BeforeEach
    void setUp() {
        contentPopularityService = new ContentPopularityService(contentPopularityRepository, memoRepository);
    }

    private void givenUriCounts(UriStat... uriCounts) {
        when(contentPopularityRepository.countRequestsPerMemoUri(any(LocalDateTime.class), anyBoolean()))
                .thenReturn(List.of(uriCounts));
    }

    private void givenMemos(Memo... memos) {
        when(memoRepository.findAllById(anyIterable())).thenReturn(List.of(memos));
    }

    private static Memo memo(long id, String name, LocalDateTime createdAt) {
        return new Memo(id, name, "content body", createdAt, createdAt);
    }

    private static Map<String, MemoTrafficStat> byUri(List<MemoTrafficStat> stats) {
        return stats.stream().collect(Collectors.toMap(MemoTrafficStat::uri, Function.identity()));
    }

    @Test
    void getPopularContent_ResolvesTitleAndCategory() {
        givenUriCounts(new UriStat("/public/memos/42", 10L));
        givenMemos(memo(42L, "/dev/java/records", LocalDateTime.now().minusDays(3)));

        List<MemoTrafficStat> stats = contentPopularityService.getPopularContentForLastDays(7, false);

        assertEquals(1, stats.size());
        MemoTrafficStat stat = stats.get(0);
        assertEquals(42L, stat.memoId());
        assertEquals("records", stat.title());
        assertEquals("/dev/java", stat.category());
        assertEquals(10L, stat.requestCount());
        assertTrue(stat.resolved());
    }

    @Test
    void getPopularContent_MergesUriVariantsOfSameMemo() {
        givenUriCounts(
                new UriStat("/public/memos/42", 10L),
                new UriStat("/public/memos/42/", 3L),
                new UriStat("/public/memos/42?page=2", 7L));
        givenMemos(memo(42L, "/dev/java/records", LocalDateTime.now().minusDays(3)));

        List<MemoTrafficStat> stats = contentPopularityService.getPopularContentForLastDays(7, false);

        assertEquals(1, stats.size());
        assertEquals("/public/memos/42", stats.get(0).uri());
        assertEquals(20L, stats.get(0).requestCount());
    }

    @Test
    void getPopularContent_PrivateMemo_FallsBackToUriAndKeepsCount() {
        givenUriCounts(new UriStat("/public/memos/7?page=1", 5L));
        givenMemos(memo(7L, "/private/draft/secret", LocalDateTime.now().minusDays(60)));

        List<MemoTrafficStat> stats = contentPopularityService.getPopularContentForLastDays(7, false);

        assertEquals(1, stats.size());
        MemoTrafficStat stat = stats.get(0);
        assertEquals("/public/memos/7", stat.uri());
        assertEquals(5L, stat.requestCount());
        assertFalse(stat.resolved());
        assertNull(stat.title());
        assertNull(stat.category());
        assertNull(stat.publishedAt());
        assertFalse(stat.evergreen());
    }

    @Test
    void getPopularContent_DeletedMemo_FallsBackToUriAndKeepsCount() {
        givenUriCounts(new UriStat("/public/memos/8", 4L));
        givenMemos(memo(8L, "/private/deleted/dev/old-post", LocalDateTime.now().minusDays(90)));

        List<MemoTrafficStat> stats = contentPopularityService.getPopularContentForLastDays(7, false);

        assertEquals(1, stats.size());
        assertEquals("/public/memos/8", stats.get(0).uri());
        assertEquals(4L, stats.get(0).requestCount());
        assertFalse(stats.get(0).resolved());
        assertNull(stats.get(0).title());
    }

    @Test
    void getPopularContent_MissingMemo_FallsBackToUriAndKeepsCount() {
        givenUriCounts(new UriStat("/public/memos/999", 6L));
        givenMemos();

        List<MemoTrafficStat> stats = contentPopularityService.getPopularContentForLastDays(7, false);

        assertEquals(1, stats.size());
        assertEquals("/public/memos/999", stats.get(0).uri());
        assertEquals(6L, stats.get(0).requestCount());
        assertFalse(stats.get(0).resolved());
    }

    @Test
    void getPopularContent_UnparseableUri_KeepsRawUriWithoutMemoLookup() {
        givenUriCounts(new UriStat("/public/memos/latest", 2L));

        List<MemoTrafficStat> stats = contentPopularityService.getPopularContentForLastDays(7, false);

        assertEquals(1, stats.size());
        assertEquals("/public/memos/latest", stats.get(0).uri());
        assertEquals(2L, stats.get(0).requestCount());
        assertFalse(stats.get(0).resolved());
        verify(memoRepository, org.mockito.Mockito.never()).findAllById(anyIterable());
    }

    @Test
    void getPopularContent_SortsByRequestCountDescending() {
        givenUriCounts(
                new UriStat("/public/memos/1", 5L),
                new UriStat("/public/memos/2", 50L),
                new UriStat("/public/memos/3", 20L));
        givenMemos(
                memo(1L, "/dev/a", LocalDateTime.now().minusDays(1)),
                memo(2L, "/dev/b", LocalDateTime.now().minusDays(1)),
                memo(3L, "/dev/c", LocalDateTime.now().minusDays(1)));

        List<MemoTrafficStat> stats = contentPopularityService.getPopularContentForLastDays(7, false);

        assertEquals(List.of(50L, 20L, 5L), stats.stream().map(MemoTrafficStat::requestCount).toList());
    }

    @Test
    void getPopularContent_MarksOldMemoAsEvergreenAndRecentMemoAsFresh() {
        givenUriCounts(
                new UriStat("/public/memos/1", 30L),
                new UriStat("/public/memos/2", 20L));
        givenMemos(
                memo(1L, "/dev/old", LocalDateTime.now().minusDays(ContentPopularityService.EVERGREEN_AGE_DAYS + 1)),
                memo(2L, "/dev/new", LocalDateTime.now().minusDays(ContentPopularityService.EVERGREEN_AGE_DAYS - 1)));

        Map<String, MemoTrafficStat> stats = byUri(contentPopularityService.getPopularContentForLastDays(7, false));

        assertTrue(stats.get("/public/memos/1").evergreen());
        assertFalse(stats.get("/public/memos/2").evergreen());
    }

    @Test
    void getPopularContent_PassesBotExclusionToRepository() {
        givenUriCounts();

        contentPopularityService.getPopularContentForLastDays(7, false);

        verify(contentPopularityRepository).countRequestsPerMemoUri(any(LocalDateTime.class), eq(false));
    }

    @Test
    void getPopularContent_PassesBotInclusionToRepository() {
        givenUriCounts();

        contentPopularityService.getPopularContentForLastDays(30, true);

        verify(contentPopularityRepository).countRequestsPerMemoUri(any(LocalDateTime.class), eq(true));
    }

    @Test
    void summarizeCategories_SumsPerCategoryAndBucketsUnresolvedTraffic() {
        givenUriCounts(
                new UriStat("/public/memos/1", 10L),
                new UriStat("/public/memos/2", 5L),
                new UriStat("/public/memos/3", 7L),
                new UriStat("/public/memos/nope", 3L));
        givenMemos(
                memo(1L, "/dev/java/records", LocalDateTime.now().minusDays(1)),
                memo(2L, "/dev/java/streams", LocalDateTime.now().minusDays(1)),
                memo(3L, "/life/diary", LocalDateTime.now().minusDays(1)));

        List<CategoryTrafficStat> categories = contentPopularityService.summarizeCategories(
                contentPopularityService.getPopularContentForLastDays(7, false));

        assertEquals(List.of(
                new CategoryTrafficStat("/dev/java", 15L),
                new CategoryTrafficStat("/life", 7L),
                new CategoryTrafficStat(ContentPopularityService.UNRESOLVED_CATEGORY, 3L)),
                categories);
    }
}
