package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RequestStatistics;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sessionises the request log at query time.
 *
 * <p>A visitor is the pair {@code (ip, user_agent)} and a session breaks after 30
 * minutes of inactivity. Both queries below derive session boundaries with window
 * functions so the database returns one summary row and one row per entry/exit
 * page instead of the raw log: a 90-day period stays a single aggregate no matter
 * how many rows it covers.
 *
 * <p>The visitor key is built inside the query and never selected, so no raw IP
 * leaves the database. Hashing it would only add cost — the key exists to group
 * rows, and nothing in the result set identifies a visitor.
 *
 * <p>SQL is restricted to the PostgreSQL/H2 intersection (window functions,
 * {@code INTERVAL '30' MINUTE}, {@code PERCENTILE_CONT}, {@code EXTRACT(EPOCH …)})
 * so the H2 test runtime exercises the same statements production runs.
 */
public interface VisitorSessionRepository extends Repository<RequestStatistics, Long> {

    String SESSION_CTE = """
            WITH visit AS (
                SELECT id,
                       COALESCE(ip, '') || '|' || COALESCE(user_agent, '') AS visitor_key,
                       uri,
                       created_at
                FROM request_statistics
                WHERE created_at >= :startDate
                  AND (:includeBots = TRUE OR is_bot = FALSE)
            ),
            boundary AS (
                SELECT id, visitor_key, uri, created_at,
                       CASE
                           WHEN LAG(created_at) OVER (PARTITION BY visitor_key ORDER BY created_at, id) IS NULL
                                OR created_at > LAG(created_at) OVER (PARTITION BY visitor_key ORDER BY created_at, id)
                                                + INTERVAL '30' MINUTE
                           THEN 1
                           ELSE 0
                       END AS session_start
                FROM visit
            ),
            session_page AS (
                SELECT id, visitor_key, uri, created_at,
                       SUM(session_start) OVER (PARTITION BY visitor_key ORDER BY created_at, id
                                                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS session_no
                FROM boundary
            )
            """;

    /**
     * One row: session count, unique visitors, returning visitors, page views,
     * single-page sessions, median session length in seconds. Median is NULL and
     * the counts are zero when the period holds no request.
     */
    @Query(nativeQuery = true, value = SESSION_CTE + """
            , per_session AS (
                SELECT visitor_key, session_no,
                       COUNT(*) AS page_views,
                       MAX(EXTRACT(EPOCH FROM created_at)) - MIN(EXTRACT(EPOCH FROM created_at)) AS duration_seconds
                FROM session_page
                GROUP BY visitor_key, session_no
            ),
            per_visitor AS (
                SELECT visitor_key, COUNT(*) AS visitor_session_count
                FROM per_session
                GROUP BY visitor_key
            )
            SELECT COUNT(*) AS session_count,
                   (SELECT COUNT(*) FROM per_visitor) AS visitor_count,
                   (SELECT COUNT(*) FROM per_visitor WHERE visitor_session_count > 1) AS returning_visitor_count,
                   SUM(page_views) AS page_view_count,
                   SUM(CASE WHEN page_views = 1 THEN 1 ELSE 0 END) AS single_page_session_count,
                   PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY duration_seconds) AS median_session_seconds
            FROM per_session
            """)
    List<Object[]> summariseSessions(@Param("startDate") LocalDateTime startDate,
                                     @Param("includeBots") boolean includeBots);

    /**
     * One row per URI that started or ended at least one session, with both counts.
     * Row count is bounded by the number of distinct pages visited in the period,
     * not by the number of requests.
     */
    @Query(nativeQuery = true, value = SESSION_CTE + """
            , ranked AS (
                SELECT uri,
                       ROW_NUMBER() OVER (PARTITION BY visitor_key, session_no ORDER BY created_at, id) AS entry_rank,
                       ROW_NUMBER() OVER (PARTITION BY visitor_key, session_no ORDER BY created_at DESC, id DESC) AS exit_rank
                FROM session_page
            )
            SELECT uri,
                   SUM(CASE WHEN entry_rank = 1 THEN 1 ELSE 0 END) AS entry_count,
                   SUM(CASE WHEN exit_rank = 1 THEN 1 ELSE 0 END) AS exit_count
            FROM ranked
            WHERE entry_rank = 1 OR exit_rank = 1
            GROUP BY uri
            """)
    List<Object[]> countEntryAndExitPages(@Param("startDate") LocalDateTime startDate,
                                         @Param("includeBots") boolean includeBots);
}
