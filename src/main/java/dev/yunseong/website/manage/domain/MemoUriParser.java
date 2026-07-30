package dev.yunseong.website.manage.domain;

/**
 * Extracts a memo id from a recorded memo detail URI.
 *
 * <p>Recorded URIs come straight off the request line, so the same memo appears
 * under several spellings: a trailing slash, a query string from the related-memo
 * pagination, a fragment, or a path parameter. Only the first path segment after
 * {@code /public/memos/} is considered, and it must be all digits, so the list
 * path {@code /public/memos} and probe paths such as {@code /public/memos/foo}
 * resolve to no memo instead of a wrong one.
 */
public final class MemoUriParser {

    private static final String MEMO_DETAIL_PREFIX = "/public/memos/";

    private MemoUriParser() {
    }

    /**
     * @return the memo id, or {@code null} when the URI is not a memo detail URI
     */
    public static Long extractMemoId(String uri) {
        if (uri == null || !uri.startsWith(MEMO_DETAIL_PREFIX)) {
            return null;
        }
        String segment = firstSegment(uri.substring(MEMO_DETAIL_PREFIX.length()));
        if (segment.isEmpty() || !segment.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException tooManyDigits) {
            return null;
        }
    }

    private static String firstSegment(String remainder) {
        for (int i = 0; i < remainder.length(); i++) {
            char delimiter = remainder.charAt(i);
            if (delimiter == '/' || delimiter == '?' || delimiter == '#' || delimiter == ';') {
                return remainder.substring(0, i);
            }
        }
        return remainder;
    }
}
