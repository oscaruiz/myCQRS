package com.oscaruiz.mycqrs.demo.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = OutboxSchemaIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class OutboxSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void createOutboxTableForTest() {
        // Tests disable Flyway; create the outbox table manually in H2.
        // This is temporary until Testcontainers Postgres (Week 5) runs real
        // migrations against a real Postgres in tests.
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS outbox (
                id UUID NOT NULL,
                aggregate_id VARCHAR(36) NOT NULL,
                event_type VARCHAR(255) NOT NULL,
                payload TEXT NOT NULL,
                occurred_at TIMESTAMP NOT NULL,
                processed_at TIMESTAMP,
                attempts INT NOT NULL DEFAULT 0,
                last_error TEXT,
                PRIMARY KEY (id)
            )
        """);
        jdbc.execute("TRUNCATE TABLE outbox");
    }

    @Test
    void outboxTableAcceptsInsertAndReturnsRow() {
        UUID id = UUID.randomUUID();
        String aggregateId = UUID.randomUUID().toString();
        String eventType = "BookCreatedEvent";
        String payload = "{\"title\":\"Test\",\"author\":\"Author\"}";
        Timestamp occurredAt = Timestamp.from(Instant.now());

        jdbc.update("""
            INSERT INTO outbox (id, aggregate_id, event_type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?)
        """, id, aggregateId, eventType, payload, occurredAt);

        Map<String, Object> row = jdbc.queryForMap(
            "SELECT * FROM outbox WHERE id = ?", id
        );

        assertEquals(aggregateId, row.get("AGGREGATE_ID"));
        assertEquals(eventType, row.get("EVENT_TYPE"));
        assertEquals(payload, row.get("PAYLOAD"));
        assertNull(row.get("PROCESSED_AT"));
        assertEquals(0, ((Number) row.get("ATTEMPTS")).intValue());
    }

    @Test
    void outboxDefaultsAttemptsToZero() {
        UUID id = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO outbox (id, aggregate_id, event_type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?)
        """, id, UUID.randomUUID().toString(), "X", "{}", Timestamp.from(Instant.now()));

        Integer attempts = jdbc.queryForObject(
            "SELECT attempts FROM outbox WHERE id = ?", Integer.class, id
        );

        assertEquals(0, attempts);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class
    })
    static class TestConfig {
    }
}
