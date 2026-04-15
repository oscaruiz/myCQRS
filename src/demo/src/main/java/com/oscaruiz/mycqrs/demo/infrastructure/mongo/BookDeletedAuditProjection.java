package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookDeletedEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@EventHandlerComponent
public class BookDeletedAuditProjection implements EventHandler<BookDeletedEvent> {

    private final BookEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public BookDeletedAuditProjection(BookEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(BookDeletedEvent event) {
        String payload = serializeEvent(event);

        BookEventLog logEntry = new BookEventLog(
                UUID.randomUUID().toString(),
                event.getAggregateId(),
                BookDeletedEvent.class.getSimpleName(),
                Instant.now(),
                BookOperation.DELETE_BOOK.name(),
                payload,
                new HashMap<>()
        );

        repository.save(logEntry);
    }

    private String serializeEvent(BookDeletedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize BookDeletedEvent", exception);
        }
    }
}
