package dev.yunseong.website.manage.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import dev.yunseong.website.manage.domain.CategoryTrafficStat;
import dev.yunseong.website.manage.domain.MemoTrafficStat;
import dev.yunseong.website.manage.domain.MemoUriParser;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.repository.ContentPopularityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns memo detail traffic into content-level statistics: title, category,
 * category totals, and the fresh/evergreen split.
 *
 * <p>Exposure is deliberately narrower than the memo visibility rules would allow
 * for an authenticated admin: only a publicly visible memo contributes its title,
 * category, and publication date. A memo that is deleted or private is handled
 * exactly like an unparseable URI — the row keeps its raw URI and its request
 * count, so no aggregate loses traffic and no non-public memo name is returned.
 * Memo content is never part of the result.
 */
@Service
@RequiredArgsConstructor
public class ContentPopularityService {

    /** A memo published longer ago than this yet still ranking counts as evergreen. */
    static final int EVERGREEN_AGE_DAYS = 30;

    /** Category bucket for traffic whose memo could not be resolved. */
    static final String UNRESOLVED_CATEGORY = "(unresolved)";

    private static final String MEMO_DETAIL_PREFIX = "/public/memos/";

    private final ContentPopularityRepository contentPopularityRepository;
    private final MemoRepository memoRepository;

    /**
     * Popular content over the last {@code days}, most requested first.
     *
     * <p>Not truncated: category totals are derived from the same list, so callers
     * decide how many rows to display.
     */
    @Transactional(readOnly = true)
    public List<MemoTrafficStat> getPopularContentForLastDays(int days, boolean includeBots) {
        LocalDateTime now = LocalDateTime.now();
        List<UriStat> uriCounts = contentPopularityRepository.countRequestsPerMemoUri(
                now.minusDays(days), includeBots);

        Map<String, Long> countsByCanonicalUri = new HashMap<>();
        Map<String, Long> memoIdsByCanonicalUri = new HashMap<>();
        for (UriStat uriCount : uriCounts) {
            Long memoId = MemoUriParser.extractMemoId(uriCount.uri());
            String canonicalUri = memoId != null ? MEMO_DETAIL_PREFIX + memoId : uriCount.uri();
            countsByCanonicalUri.merge(canonicalUri, uriCount.requestCount(), Long::sum);
            if (memoId != null) {
                memoIdsByCanonicalUri.put(canonicalUri, memoId);
            }
        }

        Map<Long, Memo> visibleMemos = findVisibleMemos(memoIdsByCanonicalUri.values());
        LocalDateTime evergreenCutoff = now.minusDays(EVERGREEN_AGE_DAYS);

        return countsByCanonicalUri.entrySet().stream()
                .map(entry -> {
                    Long memoId = memoIdsByCanonicalUri.get(entry.getKey());
                    Memo memo = memoId == null ? null : visibleMemos.get(memoId);
                    return toTrafficStat(entry.getKey(), memo, entry.getValue(), evergreenCutoff);
                })
                .sorted(Comparator.comparingLong(MemoTrafficStat::requestCount).reversed()
                        .thenComparing(MemoTrafficStat::uri))
                .toList();
    }

    public List<CategoryTrafficStat> summarizeCategories(List<MemoTrafficStat> popularContent) {
        Map<String, Long> totalsByCategory = new HashMap<>();
        for (MemoTrafficStat stat : popularContent) {
            String category = stat.resolved() ? stat.category() : UNRESOLVED_CATEGORY;
            totalsByCategory.merge(category, stat.requestCount(), Long::sum);
        }
        return totalsByCategory.entrySet().stream()
                .map(entry -> new CategoryTrafficStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(CategoryTrafficStat::requestCount).reversed()
                        .thenComparing(CategoryTrafficStat::category))
                .toList();
    }

    private Map<Long, Memo> findVisibleMemos(Collection<Long> memoIds) {
        if (memoIds.isEmpty()) {
            return Map.of();
        }
        return memoRepository.findAllById(memoIds).stream()
                .filter(memo -> !memo.isPrivate())
                .collect(Collectors.toMap(Memo::getId, Function.identity()));
    }

    private static MemoTrafficStat toTrafficStat(String uri, Memo memo, long requestCount,
                                                 LocalDateTime evergreenCutoff) {
        if (memo == null) {
            return new MemoTrafficStat(uri, null, null, null, requestCount, null, false);
        }
        LocalDateTime publishedAt = memo.getCreatedAt();
        boolean evergreen = publishedAt != null && publishedAt.isBefore(evergreenCutoff);
        return new MemoTrafficStat(uri, memo.getId(), memo.getTitle(), memo.getPath(),
                requestCount, publishedAt, evergreen);
    }
}
