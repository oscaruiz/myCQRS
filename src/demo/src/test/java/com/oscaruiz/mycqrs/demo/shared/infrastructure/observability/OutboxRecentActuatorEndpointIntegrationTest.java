package com.oscaruiz.mycqrs.demo.shared.infrastructure.observability;

import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = OutboxRecentActuatorEndpointIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class OutboxRecentActuatorEndpointIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String BOOK_EVENT_FQCN =
            "com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent";
    private static final String AUTHOR_EVENT_FQCN =
            "com.oscaruiz.mycqrs.demo.author.domain.event.AuthorCreatedEvent";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private OutboxRecentActuatorEndpoint endpoint;

    @Test
    void returnsRowsOrderedByOccurredAtDesc() {
        Instant t0 = Instant.parse("2026-04-22T10:00:00Z");
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, t0, null, 0, null);
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, t0.plusSeconds(10), null, 0, null);
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, t0.plusSeconds(5), null, 0, null);

        List<Map<String, Object>> rows = endpoint.recent(10);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("occurred_at")).isEqualTo(t0.plusSeconds(10).toString());
        assertThat(rows.get(1).get("occurred_at")).isEqualTo(t0.plusSeconds(5).toString());
        assertThat(rows.get(2).get("occurred_at")).isEqualTo(t0.toString());
    }

    @Test
    void mapsThreeStatuses() {
        Instant occurred = Instant.parse("2026-04-22T10:00:00Z");
        Instant processed = occurred.plusMillis(250);
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, occurred, null, 0, null);
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, occurred.plusSeconds(1), processed, 0, null);
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, occurred.plusSeconds(2), null, 3, "boom");

        List<Map<String, Object>> rows = endpoint.recent(10);

        assertThat(rows).extracting(r -> r.get("status"))
                .containsExactly("failed", "processed", "pending");
    }

    @Test
    void latencyMsIsProcessedMinusOccurredInMillis() {
        Instant occurred = Instant.parse("2026-04-22T10:00:00Z");
        Instant processed = occurred.plusMillis(432);
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, occurred, processed, 0, null);

        Map<String, Object> row = endpoint.recent(1).get(0);

        assertThat(((Number) row.get("latency_ms")).longValue()).isEqualTo(432L);
    }

    @Test
    void latencyMsNullWhenPending() {
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, Instant.now(), null, 0, null);

        Map<String, Object> row = endpoint.recent(1).get(0);

        assertNull(row.get("latency_ms"));
    }

    @Test
    void aggregateTypeDerivedFromEventFqcn() {
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, Instant.parse("2026-04-22T10:00:00Z"), null, 0, null);
        insert(UUID.randomUUID(), AUTHOR_EVENT_FQCN, Instant.parse("2026-04-22T09:00:00Z"), null, 0, null);

        List<Map<String, Object>> rows = endpoint.recent(10);

        assertThat(rows.get(0).get("aggregate_type")).isEqualTo("Book");
        assertThat(rows.get(0).get("event_type_simple")).isEqualTo("BookCreatedEvent");
        assertThat(rows.get(1).get("aggregate_type")).isEqualTo("Author");
        assertThat(rows.get(1).get("event_type_simple")).isEqualTo("AuthorCreatedEvent");
    }

    @Test
    void aggregateTypeUnknownForMalformedFqcn() {
        insert(UUID.randomUUID(), "NotAnFqcn", Instant.now(), null, 0, null);

        Map<String, Object> row = endpoint.recent(1).get(0);

        assertThat(row.get("aggregate_type")).isEqualTo("Unknown");
    }

    @Test
    void limitClampedToMax50() {
        Instant base = Instant.parse("2026-04-22T10:00:00Z");
        for (int i = 0; i < 60; i++) {
            insert(UUID.randomUUID(), BOOK_EVENT_FQCN, base.plusSeconds(i), null, 0, null);
        }

        assertThat(endpoint.recent(100)).hasSize(50);
    }

    @Test
    void limitClampedToMin1() {
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, Instant.now(), null, 0, null);
        insert(UUID.randomUUID(), BOOK_EVENT_FQCN, Instant.now().plusSeconds(1), null, 0, null);

        assertThat(endpoint.recent(0)).hasSize(1);
    }

    @Test
    void limitDefaultsTo10WhenNull() {
        Instant base = Instant.parse("2026-04-22T10:00:00Z");
        for (int i = 0; i < 15; i++) {
            insert(UUID.randomUUID(), BOOK_EVENT_FQCN, base.plusSeconds(i), null, 0, null);
        }

        assertThat(endpoint.recent(null)).hasSize(10);
    }

    @Test
    void aggregateTypeHelperHandlesNullAndEmpty() {
        assertThat(OutboxRecentActuatorEndpoint.aggregateTypeFromFqcn(null)).isEqualTo("Unknown");
        assertThat(OutboxRecentActuatorEndpoint.aggregateTypeFromFqcn("")).isEqualTo("Unknown");
        assertThat(OutboxRecentActuatorEndpoint.aggregateTypeFromFqcn(".domain.event.X")).isEqualTo("Unknown");
    }

    private void insert(UUID id, String eventType, Instant occurredAt,
                        Instant processedAt, int attempts, String lastError) {
        jdbc.update("""
                INSERT INTO outbox (id, aggregate_id, event_type, payload, occurred_at,
                                    processed_at, attempts, last_error)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                UUID.randomUUID().toString(),
                eventType,
                "{}",
                Timestamp.from(occurredAt),
                processedAt != null ? Timestamp.from(processedAt) : null,
                attempts,
                lastError);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class
    })
    static class TestConfig {
        @Bean
        OutboxRecentActuatorEndpoint outboxRecentActuatorEndpoint(NamedParameterJdbcTemplate jdbc) {
            return new OutboxRecentActuatorEndpoint(jdbc);
        }
    }
}
