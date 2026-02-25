package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@EventHandlerComponent
public class BookCreatedAuditProjection implements EventHandler<BookCreatedEvent> {

    private final BookEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public BookCreatedAuditProjection(BookEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(BookCreatedEvent event) {
        String payload = serializeEvent(event);

        BookEventLog logEntry = new BookEventLog(
                UUID.randomUUID().toString(),
                event.getAggregateId(),
                BookCreatedEvent.class.getSimpleName(),
                Instant.now(),
                "CREATE_BOOK",
                payload,
                new HashMap<>()
        );

        repository.save(logEntry);
    }

    private String serializeEvent(BookCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize BookCreatedEvent", exception);
        }
    }
}
