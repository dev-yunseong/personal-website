package dev.yunseong.website.blog.repository;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.yunseong.website.blog.domain.MemoMeta;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MemoFileRepository {

    private final JdbcClient jdbcClient;

    public List<MemoMeta> findAllMemoMeta() {
        return jdbcClient.sql("""
                SELECT id, name FROM memos
            """).query(
                    (rs, rowNum) ->
                            new MemoMeta(rs.getLong("id"), rs.getString("name"))
                ).list();
    }

    public boolean existPath(String fullPath) {
        return jdbcClient.sql("SELECT COUNT(*) FROM memos WHERE name = :name")
                .param("name", fullPath)
                .query(Long.class)
                .single() > 0;
    }
}
