package com.oscaruiz.mycqrs.demo.book.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class AuthorAddedToBookEvent extends DomainEvent {

    private final String authorId;

    public AuthorAddedToBookEvent(String aggregateId, String authorId) {
        super(aggregateId);
        this.authorId = authorId;
    }

    protected AuthorAddedToBookEvent(String eventId, Instant occurredAt, String aggregateId, String authorId) {
        super(eventId, occurredAt, aggregateId);
        this.authorId = authorId;
    }

    public String getAuthorId() {
        return authorId;
    }
}
