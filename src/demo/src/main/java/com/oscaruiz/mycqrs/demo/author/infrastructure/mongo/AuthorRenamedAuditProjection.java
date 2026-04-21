package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorRenamedEvent;

import java.time.Instant;
import java.util.HashMap;

@EventHandlerComponent
public class AuthorRenamedAuditProjection implements EventHandler<AuthorRenamedEvent> {

    private final AuthorEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuthorRenamedAuditProjection(AuthorEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(AuthorRenamedEvent event) {
        String payload = serialize(event);

        AuthorEventLog entry = new AuthorEventLog(
                event.getEventId(),
                event.getAggregateId(),
                AuthorRenamedEvent.class.getSimpleName(),
                Instant.now(),
                AuthorOperation.RENAME_AUTHOR.name(),
                payload,
                new HashMap<>()
        );

        repository.save(entry);
    }

    private String serialize(AuthorRenamedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize AuthorRenamedEvent", ex);
        }
    }
}
