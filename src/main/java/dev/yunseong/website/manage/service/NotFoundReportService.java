package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.NotFoundStat;
import dev.yunseong.website.manage.repository.NotFoundStatisticsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Builds the console 404 report. Request collection is centred on the public
 * route surface, so this covers 404s on real user paths rather than scanner
 * probes of unrelated paths.
 */
@Service
public class NotFoundReportService {

    public static final int PAGE_SIZE = 20;
    public static final String SORT_RECENT = "recent";

    private static final Collection<Boolean> HUMAN_TRAFFIC = List.of(false);
    private static final Collection<Boolean> ALL_TRAFFIC = List.of(false, true);

    private final NotFoundStatisticsRepository notFoundStatisticsRepository;
    private final String internalRefererPattern;

    public NotFoundReportService(NotFoundStatisticsRepository notFoundStatisticsRepository,
                                 @Value("${app.base-url}") String baseUrl) {
        this.notFoundStatisticsRepository = notFoundStatisticsRepository;
        this.internalRefererPattern = internalRefererPattern(baseUrl);
    }

    @Transactional(readOnly = true)
    public Page<NotFoundStat> getNotFoundReportForLastDays(int days, String sort, boolean includeBots, int page) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        Collection<Boolean> botFlags = includeBots ? ALL_TRAFFIC : HUMAN_TRAFFIC;
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        List<NotFoundStat> content = SORT_RECENT.equals(sort)
                ? notFoundStatisticsRepository.findNotFoundStatsByLastSeen(
                        startDate, botFlags, internalRefererPattern, pageable)
                : notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                        startDate, botFlags, internalRefererPattern, pageable);
        long distinctUris = notFoundStatisticsRepository.countNotFoundUris(startDate, botFlags);

        return new PageImpl<>(content, pageable, distinctUris);
    }

    /**
     * Referers are absolute URLs, so matching our own host is enough to separate
     * broken internal links from inbound ones. A foreign referer that embeds
     * {@code //own-host/} inside its query string is misread as internal; that is
     * rare and only affects labelling.
     */
    private static String internalRefererPattern(String baseUrl) {
        String host = URI.create(baseUrl).getHost();
        if (host == null) {
            throw new IllegalStateException("app.base-url must be an absolute URL with a host: " + baseUrl);
        }
        return "%//" + host + "/%";
    }
}
