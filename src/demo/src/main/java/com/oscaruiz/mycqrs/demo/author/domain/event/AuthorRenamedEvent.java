package com.oscaruiz.mycqrs.demo.author.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class AuthorRenamedEvent extends DomainEvent {

    private final String firstName;
    private final String lastName;

    public AuthorRenamedEvent(String aggregateId, String firstName, String lastName) {
        super(aggregateId);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    protected AuthorRenamedEvent(String eventId, Instant occurredAt, String aggregateId,
                                 String firstName, String lastName) {
        super(eventId, occurredAt, aggregateId);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
