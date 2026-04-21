package com.oscaruiz.mycqrs.demo.infrastructure.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes outbox drainage stats at {@code GET /actuator/outbox}. Read-only.
 *
 * Uses {@link NamedParameterJdbcTemplate} to match the style of
 * {@code OutboxEventBus} and {@code OutboxPoller}; sidesteps JPA entirely
 * since the outbox is not a JPA-managed entity.
 *
 * The single aggregate query relies on {@code COUNT(*) FILTER (WHERE ...)},
 * which is standard SQL and supported by Postgres (the only database used
 * in prod and in integration tests via Testcontainers).
 */
@Component
@Endpoint(id = "outbox")
public class OutboxActuatorEndpoint {

    private static final String STATS_SQL = """
            SELECT
                COUNT(*) FILTER (WHERE processed_at IS NULL)     AS pending,
                COUNT(*) FILTER (WHERE processed_at IS NOT NULL) AS processed,
                MAX(processed_at)                                AS last_processed_at
            FROM outbox
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final long pollerIntervalMs;

    public OutboxActuatorEndpoint(
            NamedParameterJdbcTemplate jdbcTemplate,
            @Value("${mycqrs.outbox.poll-interval:1000}") long pollerIntervalMs
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.pollerIntervalMs = pollerIntervalMs;
    }

    @ReadOperation
    public Map<String, Object> stats() {
        return jdbcTemplate.queryForObject(STATS_SQL, new MapSqlParameterSource(), (rs, rowNum) -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("pending", rs.getLong("pending"));
            payload.put("processed", rs.getLong("processed"));
            Timestamp lastProcessed = rs.getTimestamp("last_processed_at");
            payload.put("last_processed_at", lastProcessed != null ? lastProcessed.toInstant().toString() : null);
            payload.put("poller_interval_ms", pollerIntervalMs);
            return payload;
        });
    }
}
