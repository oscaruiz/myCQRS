package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorDeletedEvent;

import java.time.Instant;
import java.util.HashMap;

@EventHandlerComponent
public class AuthorDeletedAuditProjection implements EventHandler<AuthorDeletedEvent> {

    private final AuthorEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuthorDeletedAuditProjection(AuthorEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(AuthorDeletedEvent event) {
        String payload = serialize(event);

        AuthorEventLog entry = new AuthorEventLog(
                event.getEventId(),
                event.getAggregateId(),
                AuthorDeletedEvent.class.getSimpleName(),
                Instant.now(),
                AuthorOperation.DELETE_AUTHOR.name(),
                payload,
                new HashMap<>()
        );

        repository.save(entry);
    }

    private String serialize(AuthorDeletedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize AuthorDeletedEvent", ex);
        }
    }
}
