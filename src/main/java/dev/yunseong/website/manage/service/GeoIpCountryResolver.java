package dev.yunseong.website.manage.service;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * Resolves an IP address to an ISO 3166-1 alpha-2 country code using a local
 * MaxMind GeoLite2 database.
 *
 * <p>Never performs network I/O: the database is a memory-mapped local file and
 * anything that is not an IP literal is rejected before {@code InetAddress},
 * so a spoofed {@code X-Forwarded-For} hostname can not trigger a DNS lookup on
 * the request path.
 *
 * <p>When the database is absent — test runtime, developer machines, an image
 * built without a MaxMind licence key — every lookup returns {@code null} and
 * the rest of the application behaves unchanged.
 */
@Slf4j
@Component
public class GeoIpCountryResolver {
    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    /** Host names can not contain ':', so a match here is always an IPv6 literal. */
    private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:.]+");

    private final DatabaseReader reader;

    public GeoIpCountryResolver(@Value("${app.geoip.database-path:}") String databasePath) {
        this.reader = openDatabase(databasePath);
    }

    /** False when no usable database was found; every lookup then returns null. */
    public boolean isAvailable() {
        return reader != null;
    }

    public String resolveCountryCode(String ip) {
        if (reader == null || !isIpLiteral(ip)) {
            return null;
        }
        try {
            return reader.tryCountry(InetAddress.getByName(ip))
                    .map(response -> response.getCountry().getIsoCode())
                    .orElse(null);
        } catch (Exception e) {
            // Private ranges and addresses missing from GeoLite2 land here.
            log.debug("Country lookup failed for {}: {}", ip, e.getMessage());
            return null;
        }
    }

    /** Package-private: the guard keeps host names away from DNS, so it is tested directly. */
    static boolean isIpLiteral(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ip.indexOf(':') >= 0 ? IPV6.matcher(ip).matches() : IPV4.matcher(ip).matches();
    }

    private static DatabaseReader openDatabase(String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            log.info("GeoIP database path is not configured; country codes stay null");
            return null;
        }
        File file = new File(databasePath);
        if (!file.isFile()) {
            log.warn("GeoIP database not found at {}; country codes stay null", databasePath);
            return null;
        }
        try {
            DatabaseReader reader = new DatabaseReader.Builder(file).withCache(new CHMCache()).build();
            log.info("GeoIP database loaded from {}", databasePath);
            return reader;
        } catch (Exception e) {
            log.warn("GeoIP database at {} is unusable ({}); country codes stay null", databasePath, e.getMessage());
            return null;
        }
    }
}
