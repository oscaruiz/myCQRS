package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorCreatedEvent;

import java.time.Instant;
import java.util.HashMap;

@EventHandlerComponent
public class AuthorCreatedAuditProjection implements EventHandler<AuthorCreatedEvent> {

    private final AuthorEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuthorCreatedAuditProjection(AuthorEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(AuthorCreatedEvent event) {
        String payload = serialize(event);

        AuthorEventLog entry = new AuthorEventLog(
                event.getEventId(),
                event.getAggregateId(),
                AuthorCreatedEvent.class.getSimpleName(),
                Instant.now(),
                AuthorOperation.CREATE_AUTHOR.name(),
                payload,
                new HashMap<>()
        );

        repository.save(entry);
    }

    private String serialize(AuthorCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize AuthorCreatedEvent", ex);
        }
    }
}
