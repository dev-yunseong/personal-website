package dev.yunseong.website.manage.service;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.AsnResponse;
import dev.yunseong.website.manage.domain.AutonomousSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;

/**
 * Resolves an IP address to its autonomous system using a local MaxMind
 * GeoLite2 ASN database.
 *
 * <p>Deliberately the same shape as {@link GeoIpLocationResolver}: a
 * memory-mapped local file, never network I/O on the request path, and anything
 * that is not an IP literal rejected before {@code InetAddress} so a spoofed
 * header cannot trigger a DNS lookup.
 *
 * <p>When the database is absent — test runtime, developer machines, an image
 * built without a MaxMind licence key — every lookup returns {@code null} and
 * the datacenter signal simply never fires. Bot classification keeps working on
 * headers alone.
 */
@Slf4j
@Component
public class AsnResolver {

    private final DatabaseReader reader;

    public AsnResolver(@Value("${app.geoip.asn-database-path:}") String databasePath) {
        this.reader = openDatabase(databasePath);
    }

    /** False when no usable ASN database was found; every lookup then returns null. */
    public boolean isAvailable() {
        return reader != null;
    }

    public AutonomousSystem resolve(String ip) {
        if (reader == null || !GeoIpLocationResolver.isIpLiteral(ip)) {
            return null;
        }
        try {
            return reader.tryAsn(InetAddress.getByName(ip))
                    .map(AsnResolver::toAutonomousSystem)
                    .orElse(null);
        } catch (Exception e) {
            // Private ranges and addresses missing from GeoLite2 land here.
            log.debug("ASN lookup failed for {}: {}", ip, e.getMessage());
            return null;
        }
    }

    private static DatabaseReader openDatabase(String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            log.info("ASN database path is not configured; the datacenter signal stays off");
            return null;
        }
        File file = new File(databasePath);
        if (!file.isFile()) {
            log.warn("ASN database not found at {}; the datacenter signal stays off", databasePath);
            return null;
        }
        try {
            DatabaseReader reader = new DatabaseReader.Builder(file).withCache(new CHMCache()).build();
            if (!reader.getMetadata().getDatabaseType().contains("ASN")) {
                reader.close();
                log.warn("Database at {} is not an ASN database; the datacenter signal stays off", databasePath);
                return null;
            }
            log.info("ASN database loaded from {}", databasePath);
            return reader;
        } catch (Exception e) {
            log.warn("ASN database at {} is unusable ({}); the datacenter signal stays off",
                    databasePath, e.getMessage());
            return null;
        }
    }

    private static AutonomousSystem toAutonomousSystem(AsnResponse response) {
        return new AutonomousSystem(
                response.getAutonomousSystemNumber(),
                response.getAutonomousSystemOrganization());
    }
}
