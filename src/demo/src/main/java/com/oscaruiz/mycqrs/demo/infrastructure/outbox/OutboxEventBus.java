package com.oscaruiz.mycqrs.demo.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.Event;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * EventBus implementation that writes domain events to the outbox table within
 * the active JPA transaction, instead of invoking handlers synchronously.
 *
 * Actual dispatch to event handlers is performed asynchronously by the outbox
 * poller (introduced in Day 8). Until then, projections do not run in response
 * to commands.
 *
 * Uses NamedParameterJdbcTemplate so the SQL is resilient to schema growth:
 * adding or reordering columns does not silently break INSERT semantics the way
 * positional parameters would.
 *
 * event_type stores the fully qualified class name (FQCN) so that the Day 8
 * poller can deserialize without a type registry.
 *
 * See docs/adr/0003-outbox-pattern.md for the design rationale.
 */
public class OutboxEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventBus.class);

    private static final String INSERT_SQL = """
            INSERT INTO outbox (id, aggregate_id, event_type, payload, occurred_at)
            VALUES (:id, :aggregateId, :eventType, :payload, :occurredAt)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventBus(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends Event> void publish(T event) {
        if (!(event instanceof DomainEvent domainEvent)) {
            throw new IllegalArgumentException(
                "OutboxEventBus only handles DomainEvent instances. Got: "
                    + event.getClass().getName()
            );
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", UUID.fromString(domainEvent.getEventId()))
            .addValue("aggregateId", domainEvent.getAggregateId())
            .addValue("eventType", event.getClass().getName())
            .addValue("payload", serialize(event))
            .addValue("occurredAt", Timestamp.from(domainEvent.getOccurredAt()));

        jdbcTemplate.update(INSERT_SQL, params);

        log.debug("Wrote event {} to outbox (aggregate {})",
            event.getClass().getSimpleName(), domainEvent.getAggregateId());
    }

    @Override
    public <EventType extends Event> void registerHandler(
            Class<EventType> eventType,
            EventHandler<EventType> handler
    ) {
        log.debug("registerHandler called on OutboxEventBus for {} — ignored; "
                + "handlers register on the internal bus for poller-driven dispatch.",
            eventType.getSimpleName());
    }

    private String serialize(Event event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "Failed to serialize event " + event.getClass().getName(), e
            );
        }
    }
}
