package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.demo.integration.support.AbstractPostgresIntegrationTest;
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
class OutboxSchemaIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

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

        assertEquals(aggregateId, row.get("aggregate_id"));
        assertEquals(eventType, row.get("event_type"));
        assertEquals(payload, row.get("payload"));
        assertNull(row.get("processed_at"));
        assertEquals(0, ((Number) row.get("attempts")).intValue());
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
