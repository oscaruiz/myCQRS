package com.oscaruiz.mycqrs.demo.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.Event;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Periodically reads unprocessed events from the outbox table, publishes them
 * to the internal event bus (invoking registered projections), and marks them
 * processed on success. Failed publications increment {@code attempts} and
 * store {@code last_error} for operator visibility; the row stays in the
 * table for retry by the next poll.
 *
 * Ordering is {@code ORDER BY attempts ASC, occurred_at ASC} so a permanently
 * failing event does not block newer events indefinitely. A proper dead-letter
 * policy (terminal state after N attempts, retention, manual replay) is
 * deferred — see {@code docs/adr/0003-outbox-pattern.md}.
 *
 * {@link #poll()} is public so tests can invoke it directly instead of waiting
 * for the scheduler.
 */
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private static final String SELECT_UNPROCESSED_SQL = """
            SELECT id, event_type, payload, attempts
            FROM outbox
            WHERE processed_at IS NULL
            ORDER BY attempts ASC, occurred_at ASC
            """;

    private static final String MARK_PROCESSED_SQL = """
            UPDATE outbox
            SET processed_at = :processedAt
            WHERE id = :id
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE outbox
            SET attempts = attempts + 1,
                last_error = :lastError
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EventBus internalEventBus;

    public OutboxPoller(NamedParameterJdbcTemplate jdbcTemplate,
                        ObjectMapper objectMapper,
                        @Qualifier("internalEventBus") EventBus internalEventBus) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.internalEventBus = internalEventBus;
    }

    @Scheduled(fixedDelayString = "${mycqrs.outbox.poll-interval:1000}")
    public void poll() {
        List<OutboxRow> rows = jdbcTemplate.query(
            SELECT_UNPROCESSED_SQL,
            new MapSqlParameterSource(),
            outboxRowMapper()
        );

        if (rows.isEmpty()) {
            return;
        }

        log.debug("Polling outbox: {} pending rows", rows.size());

        for (OutboxRow row : rows) {
            dispatch(row);
        }
    }

    private void dispatch(OutboxRow row) {
        try {
            Event event = deserialize(row.eventType(), row.payload());
            internalEventBus.publish(event);
            markProcessed(row.id());
            log.debug("Dispatched outbox row {} ({})", row.id(), row.eventType());
        } catch (Exception e) {
            markFailed(row.id(), e.getMessage());
            log.warn("Failed to dispatch outbox row {} (attempt {}): {}",
                row.id(), row.attempts() + 1, e.getMessage());
        }
    }

    private Event deserialize(String eventType, String payload) throws Exception {
        Class<?> clazz = Class.forName(eventType);
        if (!Event.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException(
                "Class " + eventType + " does not implement Event");
        }
        return (Event) objectMapper.readValue(payload, clazz);
    }

    private void markProcessed(UUID id) {
        jdbcTemplate.update(MARK_PROCESSED_SQL,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("processedAt", Timestamp.from(Instant.now())));
    }

    private void markFailed(UUID id, String errorMessage) {
        jdbcTemplate.update(MARK_FAILED_SQL,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("lastError", errorMessage != null ? errorMessage : "unknown"));
    }

    private RowMapper<OutboxRow> outboxRowMapper() {
        return (rs, rowNum) -> new OutboxRow(
            UUID.fromString(rs.getString("id")),
            rs.getString("event_type"),
            rs.getString("payload"),
            rs.getInt("attempts")
        );
    }

    private record OutboxRow(UUID id, String eventType, String payload, int attempts) { }
}
