package com.oscaruiz.mycqrs.demo.book.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class BookCreatedEvent extends DomainEvent {

    private final String title;

    public BookCreatedEvent(String aggregateId, String title) {
        super(aggregateId);
        this.title = title;
    }

    protected BookCreatedEvent(String eventId, Instant occurredAt, String aggregateId, String title) {
        super(eventId, occurredAt, aggregateId);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
