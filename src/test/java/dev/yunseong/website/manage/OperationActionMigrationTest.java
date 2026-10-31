package dev.yunseong.website.manage;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationActionMigrationTest {
    @Test
    void createsQueueAndAllowsOnlyOneActiveProjectAction() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:operation-actions;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V7__operations_dashboard.sql")).execute(dataSource);
        try (var connection = dataSource.getConnection();
             var insert = connection.prepareStatement("INSERT INTO operation_actions (id, project_slug, action_key, requested_by, state, requested_at, active_key) VALUES (?, ?, ?, ?, ?, ?, ?)");
             var query = connection.createStatement()) {
            insert.setString(1, "00000000-0000-0000-0000-000000000001");
            insert.setString(2, "palworld");
            insert.setString(3, "start");
            insert.setString(4, "admin");
            insert.setString(5, "QUEUED");
            insert.setObject(6, OffsetDateTime.parse("2026-11-01T00:00:00Z"));
            insert.setString(7, "palworld:start");
            assertThat(insert.executeUpdate()).isEqualTo(1);
            insert.setString(1, "00000000-0000-0000-0000-000000000002");
            assertThatThrownBy(insert::executeUpdate)
                    .isInstanceOf(java.sql.SQLException.class);
            var rows = query.executeQuery("SELECT state, active_key FROM operation_actions");
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("state")).isEqualTo("QUEUED");
            assertThat(rows.getString("active_key")).isEqualTo("palworld:start");
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) { statement.execute("SHUTDOWN"); }
        }
    }
}
