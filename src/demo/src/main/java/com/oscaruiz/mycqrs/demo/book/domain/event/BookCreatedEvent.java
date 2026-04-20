package com.oscaruiz.mycqrs.demo.book.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class BookCreatedEvent extends DomainEvent {
    private final String title;
    private final String author;

    public BookCreatedEvent(String aggregateId, String title, String author) {
        super(aggregateId);
        this.title = title;
        this.author = author;
    }

    protected BookCreatedEvent(String eventId, Instant occurredAt, String aggregateId,
                               String title, String author) {
        super(eventId, occurredAt, aggregateId);
        this.title = title;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

}
