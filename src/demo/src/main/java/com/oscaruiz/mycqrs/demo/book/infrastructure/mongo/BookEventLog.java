package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "book_events")
public class BookEventLog {

    @Id
    private String id;

    private String aggregateId;
    private String type;
    private Instant timestamp;
    private String operation;
    private String payload;
    private Map<String, Object> metadata;

    public BookEventLog() {
    }

    public BookEventLog(
            String id,
            String aggregateId,
            String type,
            Instant timestamp,
            String operation,
            String payload,
            Map<String, Object> metadata
    ) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.type = type;
        this.timestamp = timestamp;
        this.operation = operation;
        this.payload = payload;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getOperation() {
        return operation;
    }

    public String getPayload() {
        return payload;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
