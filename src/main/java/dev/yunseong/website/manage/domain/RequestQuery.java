package dev.yunseong.website.manage.domain;

/**
 * The filter behind the request list. Every field but {@code days} is optional,
 * and {@code null} means "do not filter on this column".
 *
 * <p>Built through {@link #of}, which turns the controller's blank strings into
 * nulls, so the repository never receives {@code ""} as a value to match — an
 * empty filter box would otherwise select only rows whose column is literally
 * empty.
 */
public record RequestQuery(
        int days,
        String statusFilter,
        String uri,
        String ip,
        String userAgent,
        String referer,
        String countryCode
) {
    public static RequestQuery of(int days, String statusFilter, String uri, String ip,
                                  String userAgent, String referer, String countryCode) {
        return new RequestQuery(
                days,
                blankToNull(statusFilter),
                blankToNull(uri),
                blankToNull(ip),
                blankToNull(userAgent),
                blankToNull(referer),
                blankToNull(countryCode));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public RequestQuery withoutStatusFilter() {
        return new RequestQuery(days, null, uri, ip, userAgent, referer, countryCode);
    }

    public RequestQuery withoutUri() {
        return new RequestQuery(days, statusFilter, null, ip, userAgent, referer, countryCode);
    }

    public RequestQuery withoutIp() {
        return new RequestQuery(days, statusFilter, uri, null, userAgent, referer, countryCode);
    }

    public RequestQuery withoutUserAgent() {
        return new RequestQuery(days, statusFilter, uri, ip, null, referer, countryCode);
    }

    public RequestQuery withoutReferer() {
        return new RequestQuery(days, statusFilter, uri, ip, userAgent, null, countryCode);
    }

    public RequestQuery withoutCountryCode() {
        return new RequestQuery(days, statusFilter, uri, ip, userAgent, referer, null);
    }
}
