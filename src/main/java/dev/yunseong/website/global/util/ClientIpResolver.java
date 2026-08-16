package dev.yunseong.website.global.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * Resolves the address of the real visitor rather than the proxy in front of us.
 *
 * <p>Behind Cloudflare, {@code getRemoteAddr()} is the edge IP: Tomcat's
 * {@code RemoteIpValve} — enabled by {@code forward-headers-strategy: native} —
 * only walks past proxies matching its {@code internalProxies} pattern, which
 * covers private ranges only, and every Cloudflare edge address is public. So
 * the visitor address is read from {@code CF-Connecting-IP}, which Cloudflare
 * sets on every proxied request and overwrites if the client supplied one.
 *
 * <p>The header is only trustworthy while the origin refuses non-Cloudflare
 * traffic; a direct request can forge it. The IP-literal check below bounds the
 * damage — a forged value still has to be an address, so it cannot widen the
 * {@code ip} column or reach {@code InetAddress} as a host name — but it is not
 * a substitute for restricting the origin.
 */
public final class ClientIpResolver {
    public static final String CLOUDFLARE_HEADER = "CF-Connecting-IP";

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    /** Host names can not contain ':', so a match here is always an IPv6 literal. */
    private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:.]+");

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(CLOUDFLARE_HEADER);
        return isIpLiteral(forwarded) ? forwarded.trim() : request.getRemoteAddr();
    }

    private static boolean isIpLiteral(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return trimmed.indexOf(':') >= 0 ? IPV6.matcher(trimmed).matches() : IPV4.matcher(trimmed).matches();
    }
}
