package com.oscaruiz.mycqrs.demo.shared.infrastructure.observability;

import org.bson.Document;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exposes a per-entity write/read side-by-side view at
 * {@code GET /actuator/entity-snapshot/{kind}/{id}} where {@code kind} is
 * {@code book} or {@code author}. Read-only.
 *
 * Talks to Postgres via raw JDBC and to Mongo via {@link MongoTemplate} —
 * deliberately bypassing the aggregate adapters in {@code book.infrastructure}
 * and {@code author.infrastructure}. Observability tooling has no business
 * reaching through a domain adapter: it inspects the persistence layer
 * directly, the same pattern as {@link OutboxActuatorEndpoint} and
 * {@link OutboxRecentActuatorEndpoint}. This keeps the per-aggregate onion
 * intact (enforced by {@code ArchitectureTest}).
 *
 * Response shape:
 * <pre>
 *   { postgres: { ... } | null,
 *     mongo:    { ... } | null,
 *     events:   [ { id, type, operation, timestamp }, ... up to 10 ] }
 * </pre>
 *
 * Any of the three can be null / empty when the corresponding store has
 * no data for that id — essential for visualising the consistency window
 * between a write and the projection catching up.
 *
 * Part of the observability contract declared by ADR 0010. Renaming or
 * removing the columns / collections it reads requires a superseding ADR.
 */
@Component
@Endpoint(id = "entity-snapshot")
public class EntitySnapshotActuatorEndpoint {

    private static final int EVENT_LIMIT = 10;

    private static final String BOOK_ROW_SQL = """
            SELECT id, title, deleted, version FROM book_entity WHERE id = :id
            """;
    private static final String BOOK_AUTHORS_SQL = """
            SELECT author_id FROM book_authors WHERE book_id = :id ORDER BY author_id
            """;
    private static final String AUTHOR_ROW_SQL = """
            SELECT id, first_name, last_name, birth_year, deleted, version
            FROM author_entity WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final MongoTemplate mongo;

    public EntitySnapshotActuatorEndpoint(NamedParameterJdbcTemplate jdbc, MongoTemplate mongo) {
        this.jdbc = jdbc;
        this.mongo = mongo;
    }

    @ReadOperation
    public Map<String, Object> fetch(@Selector String kind, @Selector String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return Map.of("error", "invalid id");
        }

        return switch (kind) {
            case "book" -> bookSnapshot(uuid);
            case "author" -> authorSnapshot(uuid);
            default -> Map.of("error", "unknown kind");
        };
    }

    private Map<String, Object> bookSnapshot(UUID id) {
        return payload(
                readBookRow(id),
                readMongoDoc("books", id.toString()),
                readEvents("book_events", id.toString())
        );
    }

    private Map<String, Object> authorSnapshot(UUID id) {
        return payload(
                readAuthorRow(id),
                readMongoDoc("authors", id.toString()),
                readEvents("author_events", id.toString())
        );
    }

    private Map<String, Object> readBookRow(UUID id) {
        var params = new MapSqlParameterSource("id", id);
        List<Map<String, Object>> rows = jdbc.query(BOOK_ROW_SQL, params, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("title", rs.getString("title"));
            row.put("deleted", rs.getBoolean("deleted"));
            row.put("version", rs.getObject("version"));
            return row;
        });
        if (rows.isEmpty()) return null;
        Map<String, Object> row = rows.get(0);
        row.put("author_ids", jdbc.query(BOOK_AUTHORS_SQL, params, (rs, n) -> rs.getString("author_id")));
        return row;
    }

    private Map<String, Object> readAuthorRow(UUID id) {
        var params = new MapSqlParameterSource("id", id);
        List<Map<String, Object>> rows = jdbc.query(AUTHOR_ROW_SQL, params, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("first_name", rs.getString("first_name"));
            row.put("last_name", rs.getString("last_name"));
            int birthYear = rs.getInt("birth_year");
            row.put("birth_year", rs.wasNull() ? null : birthYear);
            row.put("deleted", rs.getBoolean("deleted"));
            row.put("version", rs.getObject("version"));
            return row;
        });
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> readMongoDoc(String collection, String id) {
        Document doc = mongo.getCollection(collection).find(new Document("_id", id)).first();
        return doc == null ? null : new LinkedHashMap<>(doc);
    }

    private List<Map<String, Object>> readEvents(String collection, String aggregateId) {
        Query query = Query.query(Criteria.where("aggregateId").is(aggregateId))
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .limit(EVENT_LIMIT);
        List<Document> docs = mongo.find(query, Document.class, collection);
        List<Map<String, Object>> out = new ArrayList<>(docs.size());
        for (Document d : docs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", d.getString("_id"));
            row.put("type", d.getString("type"));
            row.put("operation", d.getString("operation"));
            Object ts = d.get("timestamp");
            row.put("timestamp", ts instanceof Date date
                    ? date.toInstant().toString()
                    : ts instanceof Instant inst ? inst.toString() : null);
            out.add(row);
        }
        return out;
    }

    private Map<String, Object> payload(Map<String, Object> postgres,
                                        Map<String, Object> mongoDoc,
                                        List<Map<String, Object>> events) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("postgres", postgres);
        out.put("mongo", mongoDoc);
        out.put("events", events);
        return out;
    }
}
