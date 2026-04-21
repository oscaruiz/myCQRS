package com.oscaruiz.mycqrs.demo.book.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class AuthorRemovedFromBookEvent extends DomainEvent {

    private final String authorId;

    public AuthorRemovedFromBookEvent(String aggregateId, String authorId) {
        super(aggregateId);
        this.authorId = authorId;
    }

    protected AuthorRemovedFromBookEvent(String eventId, Instant occurredAt, String aggregateId, String authorId) {
        super(eventId, occurredAt, aggregateId);
        this.authorId = authorId;
    }

    public String getAuthorId() {
        return authorId;
    }
}
