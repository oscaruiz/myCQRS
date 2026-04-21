package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorAddedToBookEvent;

import java.time.Instant;
import java.util.HashMap;

@EventHandlerComponent
public class AuthorAddedToBookAuditProjection implements EventHandler<AuthorAddedToBookEvent> {

    private final BookEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuthorAddedToBookAuditProjection(BookEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(AuthorAddedToBookEvent event) {
        String payload = serialize(event);

        BookEventLog entry = new BookEventLog(
                event.getEventId(),
                event.getAggregateId(),
                AuthorAddedToBookEvent.class.getSimpleName(),
                Instant.now(),
                BookOperation.ADD_AUTHOR_TO_BOOK.name(),
                payload,
                new HashMap<>()
        );

        repository.save(entry);
    }

    private String serialize(AuthorAddedToBookEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize AuthorAddedToBookEvent", ex);
        }
    }
}
