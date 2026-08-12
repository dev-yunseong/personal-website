package dev.yunseong.website.briefing.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Daily briefings an external agent publishes, stored as private memos.
 *
 * <p>A briefing is a memo named {@code /private/briefing/<kind>/<yyyy-MM-dd>}. That
 * one decision carries the whole feature:
 *
 * <ul>
 *   <li><b>No new persistence.</b> No entity, no table, no migration — a briefing is
 *       a memo, and everything memos already do (edit, soft delete, full-text search
 *       for the author) works on it unchanged.
 *   <li><b>The {@code /private} prefix is the blocking mechanism.</b> Because it is
 *       the same prefix {@link Memo#PRIVATE_PREFIX} already means, the blog list, the
 *       category tree, search, and the sitemap keep briefings out with no filter code
 *       written for them specifically.
 *   <li><b>A kind is only a path segment.</b> Adding {@code jobs} next to {@code news}
 *       — or anything after that — is one POST under a new name. There is no registry
 *       to update and no code here that enumerates kinds.
 * </ul>
 *
 * <p>Reads deliberately use {@link MemoService#findMemoWithoutVisibilityFilter} and
 * {@link MemoService#getMemosByNamePrefix}, which do not apply the visibility filter,
 * because the public digest page has to render memos that are private by design. The
 * prefix pinned in {@link #BRIEFING_ROOT} is therefore the only thing standing between
 * that page and every other private memo — see {@link #briefingPrefix()}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefingService {

    /** Every briefing lives under here, and nothing else does. */
    public static final String BRIEFING_ROOT = Memo.PRIVATE_PREFIX + "/briefing";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * A kind becomes a path segment, so it is restricted to what reads well in a URL and
     * cannot escape {@link #BRIEFING_ROOT}: no slash, no dot, no percent-encoding, no
     * leading hyphen.
     */
    private static final Pattern KIND_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    /** Matches the curator memory limit; a briefing is a day's reading, not an archive. */
    private static final int MAX_BODY_LENGTH = 20_000;

    /**
     * How many of the most recent briefing memos the kind and date listings look at.
     * At a handful of kinds a day this is months of history, and it keeps the listing a
     * single bounded query instead of a growing table scan.
     */
    private static final int RECENT_SCAN_LIMIT = 120;

    private static final Pattern LEADING_H1 = Pattern.compile("^\\s*#\\s+(.+?)\\s*$", Pattern.MULTILINE);

    private final MemoService memoService;

    /**
     * Stores one briefing, replacing that kind's entry for that date if it already exists.
     *
     * <p>Re-publishing is an update rather than a new post, which is what makes the agent
     * side safe to retry: a cron job that fires twice, or a run that is corrected an hour
     * later, leaves one memo per kind per day.
     *
     * @return the stored memo
     */
    @Transactional
    public Memo publish(String kind, LocalDate date, String body) {
        String normalizedKind = normalizeKind(kind);
        LocalDate briefingDate = date == null ? LocalDate.now() : date;
        String normalizedBody = normalizeBody(body);

        String name = briefingName(normalizedKind, briefingDate);
        return memoService.upsertMemo(name, wrap(normalizedKind, briefingDate, normalizedBody));
    }

    public Optional<Memo> findByKindAndDate(String kind, LocalDate date) {
        return memoService.findMemoWithoutVisibilityFilter(briefingName(normalizeKind(kind), date));
    }

    /** The most recent briefing of one kind, by date rather than by write time. */
    public Optional<Memo> findLatest(String kind) {
        String prefix = briefingPrefix() + normalizeKind(kind) + "/";
        return memoService.getMemosByNamePrefix(prefix, RECENT_SCAN_LIMIT).stream()
                .max(Comparator.comparing((Memo memo) -> dateOf(memo).orElse(LocalDate.MIN)));
    }

    /**
     * Every briefing published on one date, ordered by kind so the digest page reads the
     * same way every morning.
     *
     * <p>Resolved as one exact lookup per known kind rather than by filtering the recent
     * window. Kinds turn over far more slowly than dates do, so this keeps an old
     * {@code ?date=} working long after that day has scrolled out of the window.
     */
    public List<Memo> findByDate(LocalDate date) {
        return listKinds().stream()
                .flatMap(kind -> memoService
                        .findMemoWithoutVisibilityFilter(briefingName(kind, date))
                        .stream())
                .toList();
    }

    /**
     * Kinds seen in recent history, sorted. Derived from the stored names rather than a
     * registry — that is what lets a new kind exist the moment it is first published.
     */
    public List<String> listKinds() {
        return recentBriefings().stream()
                .flatMap(memo -> kindOf(memo).stream())
                .distinct()
                .sorted()
                .toList();
    }

    /** Dates that have at least one briefing, newest first. */
    public List<LocalDate> recentDates(int limit) {
        return recentBriefings().stream()
                .flatMap(memo -> dateOf(memo).stream())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .limit(Math.max(limit, 0))
                .toList();
    }

    /**
     * The kind of a briefing memo, or empty when the name is not shaped like one.
     *
     * <p>Public so the digest template can label an entry without re-deriving the path
     * convention.
     */
    public Optional<String> kindOf(Memo memo) {
        return segmentsOf(memo).map(segments -> segments[0]);
    }

    public Optional<LocalDate> dateOf(Memo memo) {
        return segmentsOf(memo).flatMap(segments -> parseDate(segments[1]));
    }

    public static LocalDate parseRequiredDate(String date) {
        return parseDate(date)
                .orElseThrow(() -> new IllegalArgumentException("Date must be yyyy-MM-dd."));
    }

    private List<Memo> recentBriefings() {
        return memoService.getMemosByNamePrefix(briefingPrefix(), RECENT_SCAN_LIMIT).stream()
                .filter(memo -> segmentsOf(memo).isPresent())
                .toList();
    }

    /**
     * Splits a stored name into {@code [kind, date]}, or empty when it is not a briefing.
     *
     * <p>The {@code startsWith} check is what keeps every read confined to
     * {@link #BRIEFING_ROOT}. Loosening it would hand the public digest page memos the
     * visibility filter was supposed to hide.
     */
    private Optional<String[]> segmentsOf(Memo memo) {
        String name = memo == null ? null : memo.getName();
        if (name == null || !name.startsWith(briefingPrefix())) {
            return Optional.empty();
        }
        String[] segments = name.substring(briefingPrefix().length()).split("/");
        if (segments.length != 2 || !KIND_PATTERN.matcher(segments[0]).matches()) {
            return Optional.empty();
        }
        return Optional.of(segments);
    }

    private static Optional<LocalDate> parseDate(String date) {
        if (date == null || date.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(date.trim(), DATE_FORMATTER));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String briefingPrefix() {
        return BRIEFING_ROOT + "/";
    }

    private static String briefingName(String kind, LocalDate date) {
        return briefingPrefix() + kind + "/" + date.format(DATE_FORMATTER);
    }

    private static String normalizeKind(String kind) {
        String normalized = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
        if (!KIND_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Kind must be lowercase letters, digits, and hyphens (1-32 characters).");
        }
        return normalized;
    }

    private static String normalizeBody(String body) {
        String normalized = body == null ? "" : body.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Briefing body is required.");
        }
        if (normalized.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException(
                    "Briefing body is too long (max " + MAX_BODY_LENGTH + " characters).");
        }
        return normalized;
    }

    /**
     * Gives every briefing the same shape regardless of which agent wrote it: one title,
     * one meta line saying what this is, then the body.
     *
     * <p>The title is the body's first {@code #} heading, which is what an agent writing
     * markdown produces anyway — so the convention does the work instead of a separate
     * title field in the request. That heading is then lifted out of the body so it is
     * not printed twice.
     */
    private static String wrap(String kind, LocalDate date, String body) {
        String title = extractTitle(body).orElseGet(() -> kind + " " + date.format(DATE_FORMATTER));
        String remainder = stripLeadingH1(body);
        return """
                # %s

                `%s` · %s · 에이전트 작성

                %s
                """.formatted(title, kind, date.format(DATE_FORMATTER), remainder);
    }

    private static Optional<String> extractTitle(String body) {
        var matcher = LEADING_H1.matcher(body);
        if (matcher.find() && matcher.start() == 0) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    private static String stripLeadingH1(String body) {
        var matcher = LEADING_H1.matcher(body);
        if (matcher.find() && matcher.start() == 0) {
            return body.substring(matcher.end()).strip();
        }
        return body;
    }
}
