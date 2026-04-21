package com.oscaruiz.mycqrs.demo.book.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class BookUpdatedEvent extends DomainEvent {

    private final String title;

    public BookUpdatedEvent(String aggregateId, String title) {
        super(aggregateId);
        this.title = title;
    }

    protected BookUpdatedEvent(String eventId, Instant occurredAt, String aggregateId, String title) {
        super(eventId, occurredAt, aggregateId);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
