package dev.yunseong.website.manage;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleMigrationTest {
    @Test
    void createsPersistentLoginAndProjectReportTables() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:console-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        try {
            new ResourceDatabasePopulator(new ClassPathResource(
                    "db/migration/V6__admin_console_and_persistent_login.sql")).execute(dataSource);

            OffsetDateTime lastUsed = OffsetDateTime.parse("2026-10-25T12:34:56+09:00");
            OffsetDateTime updatedAt = OffsetDateTime.parse("2026-10-25T03:34:56Z");
            String payload = "{\"name\":\"Example\",\"description\":\"Migration test\",\"tags\":[\"java\",\"web\"]}";
            try (var connection = dataSource.getConnection();
                 var login = connection.prepareStatement(
                         "INSERT INTO persistent_logins (series, username, token, last_used) VALUES (?, ?, ?, ?)");
                 var report = connection.prepareStatement(
                         "INSERT INTO project_reports (slug, payload, updated_at) VALUES (?, ?, ?)")) {
                login.setString(1, "series-001");
                login.setString(2, "admin");
                login.setString(3, "token-001");
                login.setObject(4, lastUsed);
                assertThat(login.executeUpdate()).isEqualTo(1);

                report.setString(1, "example-project");
                report.setString(2, payload);
                report.setObject(3, updatedAt);
                assertThat(report.executeUpdate()).isEqualTo(1);
            }

            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 var logins = statement.executeQuery("SELECT username, token, last_used FROM persistent_logins")) {
                assertThat(logins.next()).isTrue();
                assertThat(logins.getString("username")).isEqualTo("admin");
                assertThat(logins.getString("token")).isEqualTo("token-001");
                assertThat(logins.getObject("last_used", OffsetDateTime.class)).isEqualTo(lastUsed);
            }
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 var reports = statement.executeQuery("SELECT slug, payload, updated_at FROM project_reports")) {
                assertThat(reports.next()).isTrue();
                assertThat(reports.getString("slug")).isEqualTo("example-project");
                assertThat(reports.getString("payload")).isEqualTo(payload);
                assertThat(reports.getObject("updated_at", OffsetDateTime.class)).isEqualTo(updatedAt);
            }
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }
}
