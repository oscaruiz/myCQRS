package com.oscaruiz.mycqrs.demo.author.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class AuthorDeletedEvent extends DomainEvent {

    public AuthorDeletedEvent(String aggregateId) {
        super(aggregateId);
    }

    protected AuthorDeletedEvent(String eventId, Instant occurredAt, String aggregateId) {
        super(eventId, occurredAt, aggregateId);
    }
}
