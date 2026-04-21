package com.oscaruiz.mycqrs.demo.author.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class AuthorCreatedEvent extends DomainEvent {

    private final String firstName;
    private final String lastName;
    private final Integer birthYear;

    public AuthorCreatedEvent(String aggregateId, String firstName, String lastName, Integer birthYear) {
        super(aggregateId);
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
    }

    protected AuthorCreatedEvent(String eventId, Instant occurredAt, String aggregateId,
                                 String firstName, String lastName, Integer birthYear) {
        super(eventId, occurredAt, aggregateId);
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Integer getBirthYear() {
        return birthYear;
    }
}
