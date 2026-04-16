package com.oscaruiz.mycqrs.demo.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class BookDeletedEvent extends DomainEvent {

    public BookDeletedEvent(String aggregateId) {
        super(aggregateId);
    }

    protected BookDeletedEvent(String eventId, Instant occurredAt, String aggregateId) {
        super(eventId, occurredAt, aggregateId);
    }
}
