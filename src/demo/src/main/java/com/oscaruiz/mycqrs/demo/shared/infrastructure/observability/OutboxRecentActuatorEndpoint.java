package com.oscaruiz.mycqrs.demo.shared.infrastructure.observability;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the most recent outbox rows at {@code GET /actuator/outbox-recent}.
 * Read-only. Paired with {@link OutboxActuatorEndpoint}: {@code /actuator/outbox}
 * returns aggregate counters, this endpoint returns the last N rows with
 * per-event status and latency.
 *
 * The {@code aggregate_type} field is derived from {@code event_type} at query
 * time because the outbox schema does not carry an {@code aggregate_type}
 * column (see ADR 0003). The convention {@code ...<aggregate>.domain.event.<Event>}
 * is enforced by ArchUnit; non-conforming FQCNs fall back to "Unknown".
 *
 * See docs/adr/0009-dashboard-as-first-class-observability-surface.md for the
 * observability-contract rationale.
 */
@Component
@Endpoint(id = "outbox-recent")
public class OutboxRecentActuatorEndpoint {

    static final int DEFAULT_LIMIT = 10;
    static final int MIN_LIMIT = 1;
    static final int MAX_LIMIT = 50;

    private static final String RECENT_SQL = """
            SELECT id, event_type, occurred_at, processed_at, attempts, last_error
            FROM outbox
            ORDER BY occurred_at DESC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OutboxRecentActuatorEndpoint(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @ReadOperation
    public List<Map<String, Object>> recent(@Nullable Integer limit) {
        int clamped = clamp(limit != null ? limit : DEFAULT_LIMIT);
        MapSqlParameterSource params = new MapSqlParameterSource("limit", clamped);
        return jdbcTemplate.query(RECENT_SQL, params, (rs, rowNum) -> {
            String eventType = rs.getString("event_type");
            Timestamp occurredAt = rs.getTimestamp("occurred_at");
            Timestamp processedAt = rs.getTimestamp("processed_at");
            int attempts = rs.getInt("attempts");
            String lastError = rs.getString("last_error");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("event_type", eventType);
            row.put("event_type_simple", simpleName(eventType));
            row.put("aggregate_type", aggregateTypeFromFqcn(eventType));
            row.put("occurred_at", occurredAt != null ? occurredAt.toInstant().toString() : null);
            row.put("processed_at", processedAt != null ? processedAt.toInstant().toString() : null);
            row.put("latency_ms", latencyMs(occurredAt, processedAt));
            row.put("status", status(attempts, lastError, processedAt));
            return row;
        });
    }

    static int clamp(int v) {
        if (v < MIN_LIMIT) return MIN_LIMIT;
        if (v > MAX_LIMIT) return MAX_LIMIT;
        return v;
    }

    static String simpleName(String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) return fqcn;
        int last = fqcn.lastIndexOf('.');
        return last < 0 ? fqcn : fqcn.substring(last + 1);
    }

    static String aggregateTypeFromFqcn(String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) return "Unknown";
        int marker = fqcn.indexOf(".domain.event.");
        if (marker <= 0) return "Unknown";
        String head = fqcn.substring(0, marker);
        int lastDot = head.lastIndexOf('.');
        String segment = lastDot < 0 ? head : head.substring(lastDot + 1);
        if (segment.isEmpty()) return "Unknown";
        return Character.toUpperCase(segment.charAt(0)) + segment.substring(1);
    }

    static Long latencyMs(Timestamp occurredAt, Timestamp processedAt) {
        if (occurredAt == null || processedAt == null) return null;
        return processedAt.getTime() - occurredAt.getTime();
    }

    static String status(int attempts, String lastError, Timestamp processedAt) {
        if (attempts > 0 && lastError != null) return "failed";
        if (processedAt != null) return "processed";
        return "pending";
    }
}
