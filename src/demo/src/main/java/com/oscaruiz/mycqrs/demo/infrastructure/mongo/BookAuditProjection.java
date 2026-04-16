package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;

import java.time.Instant;
import java.util.HashMap;

/**
 * Writes a BookEventLog entry for every BookUpdatedEvent.
 *
 * Idempotency: the event ID is used as the Mongo document _id, so re-processing
 * the same event (after an outbox retry) silently upserts rather than creating
 * a duplicate log entry. Operational visibility of retries lives in the outbox
 * table's attempts column.
 */
@EventHandlerComponent
public class BookAuditProjection implements EventHandler<BookUpdatedEvent> {

    private final BookEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public BookAuditProjection(BookEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(BookUpdatedEvent event) {
        String payload = serializeEvent(event);

        BookEventLog logEntry = new BookEventLog(
                event.getEventId(),
                event.getAggregateId(),
                BookUpdatedEvent.class.getSimpleName(),
                Instant.now(),
                "UPDATE_BOOK",
                payload,
                new HashMap<>()
        );

        repository.save(logEntry);
    }

    private String serializeEvent(BookUpdatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize BookUpdatedEvent", exception);
        }
    }
}
