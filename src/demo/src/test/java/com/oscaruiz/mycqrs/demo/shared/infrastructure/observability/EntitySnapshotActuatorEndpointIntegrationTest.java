package com.oscaruiz.mycqrs.demo.shared.infrastructure.observability;

import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EntitySnapshotActuatorEndpointIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class EntitySnapshotActuatorEndpointIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired private EntitySnapshotActuatorEndpoint endpoint;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MongoTemplate mongo;

    @Test
    void bookPresentInBothStoresWithEvents() {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        insertBookRow(bookId, "Ficciones", false, 0L);
        insertBookAuthor(bookId, authorId);
        mongo.getCollection("books").insertOne(new Document()
                .append("_id", bookId.toString())
                .append("title", "Ficciones")
                .append("authors", List.of(new Document()
                        .append("authorId", authorId.toString())
                        .append("fullName", "Jorge Borges")
                        .append("retired", false))));
        insertBookEvent(bookId, "BookCreatedEvent", "CREATE", Instant.parse("2026-04-22T10:00:00Z"));
        insertBookEvent(bookId, "AuthorAddedToBookEvent", "UPDATE", Instant.parse("2026-04-22T10:00:05Z"));

        Map<String, Object> snapshot = endpoint.fetch("book", bookId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> postgres = (Map<String, Object>) snapshot.get("postgres");
        assertThat(postgres).isNotNull();
        assertThat(postgres.get("title")).isEqualTo("Ficciones");
        assertThat(postgres.get("deleted")).isEqualTo(false);
        assertThat(postgres.get("author_ids")).isEqualTo(List.of(authorId.toString()));

        @SuppressWarnings("unchecked")
        Map<String, Object> mongoDoc = (Map<String, Object>) snapshot.get("mongo");
        assertThat(mongoDoc).isNotNull();
        assertThat(mongoDoc.get("title")).isEqualTo("Ficciones");
        assertThat((List<?>) mongoDoc.get("authors")).hasSize(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) snapshot.get("events");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).get("type")).isEqualTo("AuthorAddedToBookEvent");
        assertThat(events.get(1).get("type")).isEqualTo("BookCreatedEvent");
    }

    @Test
    void bookPresentOnlyInPostgres() {
        UUID bookId = UUID.randomUUID();
        insertBookRow(bookId, "Solo Postgres", false, 0L);

        Map<String, Object> snapshot = endpoint.fetch("book", bookId.toString());

        assertThat(snapshot.get("postgres")).isNotNull();
        assertThat(snapshot.get("mongo")).isNull();
        assertThat((List<?>) snapshot.get("events")).isEmpty();
    }

    @Test
    void bookUnknownIdReturnsNullsAndEmptyEvents() {
        Map<String, Object> snapshot = endpoint.fetch("book", UUID.randomUUID().toString());

        assertThat(snapshot.get("postgres")).isNull();
        assertThat(snapshot.get("mongo")).isNull();
        assertThat((List<?>) snapshot.get("events")).isEmpty();
    }

    @Test
    void authorPresentInBothStoresWithEvents() {
        UUID authorId = UUID.randomUUID();
        insertAuthorRow(authorId, "Jorge", "Borges", 1899, false, 0L);
        mongo.getCollection("authors").insertOne(new Document()
                .append("_id", authorId.toString())
                .append("firstName", "Jorge")
                .append("lastName", "Borges")
                .append("birthYear", 1899)
                .append("deleted", false)
                .append("books", List.of(new Document()
                        .append("bookId", UUID.randomUUID().toString())
                        .append("title", "Ficciones"))));
        insertAuthorEvent(authorId, "AuthorCreatedEvent", "CREATE", Instant.parse("2026-04-22T10:00:00Z"));

        Map<String, Object> snapshot = endpoint.fetch("author", authorId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> postgres = (Map<String, Object>) snapshot.get("postgres");
        assertThat(postgres).isNotNull();
        assertThat(postgres.get("first_name")).isEqualTo("Jorge");
        assertThat(postgres.get("last_name")).isEqualTo("Borges");
        assertThat(postgres.get("birth_year")).isEqualTo(1899);

        @SuppressWarnings("unchecked")
        Map<String, Object> mongoDoc = (Map<String, Object>) snapshot.get("mongo");
        assertThat(mongoDoc).isNotNull();
        assertThat(mongoDoc.get("firstName")).isEqualTo("Jorge");
        assertThat((List<?>) mongoDoc.get("books")).hasSize(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) snapshot.get("events");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("type")).isEqualTo("AuthorCreatedEvent");
    }

    @Test
    void authorPresentOnlyInPostgres() {
        UUID authorId = UUID.randomUUID();
        insertAuthorRow(authorId, "Solo", "Postgres", null, false, 0L);

        Map<String, Object> snapshot = endpoint.fetch("author", authorId.toString());

        assertThat(snapshot.get("postgres")).isNotNull();
        assertThat(snapshot.get("mongo")).isNull();
        assertThat((List<?>) snapshot.get("events")).isEmpty();
    }

    @Test
    void authorUnknownIdReturnsNullsAndEmptyEvents() {
        Map<String, Object> snapshot = endpoint.fetch("author", UUID.randomUUID().toString());

        assertThat(snapshot.get("postgres")).isNull();
        assertThat(snapshot.get("mongo")).isNull();
        assertThat((List<?>) snapshot.get("events")).isEmpty();
    }

    @Test
    void unknownKindReturnsErrorPayload() {
        Map<String, Object> snapshot = endpoint.fetch("widget", UUID.randomUUID().toString());

        assertThat(snapshot).containsEntry("error", "unknown kind");
        assertThat(snapshot).doesNotContainKeys("postgres", "mongo", "events");
    }

    @Test
    void invalidIdReturnsErrorPayload() {
        Map<String, Object> snapshot = endpoint.fetch("book", "not-a-uuid");

        assertThat(snapshot).containsEntry("error", "invalid id");
        assertThat(snapshot).doesNotContainKeys("postgres", "mongo", "events");
    }

    @Test
    void eventLogIsLimitedToTenNewestFirst() {
        UUID bookId = UUID.randomUUID();
        Instant base = Instant.parse("2026-04-22T10:00:00Z");
        for (int i = 0; i < 15; i++) {
            insertBookEvent(bookId, "E" + i, "UPDATE", base.plusSeconds(i));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) endpoint
                .fetch("book", bookId.toString()).get("events");

        assertThat(events).hasSize(10);
        assertThat(events.get(0).get("type")).isEqualTo("E14");
        assertThat(events.get(9).get("type")).isEqualTo("E5");
    }

    private void insertBookRow(UUID id, String title, boolean deleted, long version) {
        jdbc.update("INSERT INTO book_entity (id, title, deleted, version) VALUES (?, ?, ?, ?)",
                id, title, deleted, version);
    }

    private void insertBookAuthor(UUID bookId, UUID authorId) {
        jdbc.update("INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)", bookId, authorId);
    }

    private void insertAuthorRow(UUID id, String first, String last, Integer birth,
                                 boolean deleted, long version) {
        jdbc.update("""
                INSERT INTO author_entity (id, first_name, last_name, birth_year, deleted, version)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, first, last, birth, deleted, version);
    }

    private void insertBookEvent(UUID aggregateId, String type, String operation, Instant timestamp) {
        mongo.getCollection("book_events").insertOne(new Document()
                .append("_id", UUID.randomUUID().toString())
                .append("aggregateId", aggregateId.toString())
                .append("type", type)
                .append("operation", operation)
                .append("timestamp", Date.from(timestamp))
                .append("payload", "{}"));
    }

    private void insertAuthorEvent(UUID aggregateId, String type, String operation, Instant timestamp) {
        mongo.getCollection("author_events").insertOne(new Document()
                .append("_id", UUID.randomUUID().toString())
                .append("aggregateId", aggregateId.toString())
                .append("type", type)
                .append("operation", operation)
                .append("timestamp", Date.from(timestamp))
                .append("payload", "{}"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {
        @Bean
        EntitySnapshotActuatorEndpoint entitySnapshotActuatorEndpoint(
                NamedParameterJdbcTemplate jdbc, MongoTemplate mongo) {
            return new EntitySnapshotActuatorEndpoint(jdbc, mongo);
        }
    }
}
