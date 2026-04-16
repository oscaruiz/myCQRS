package com.oscaruiz.mycqrs.demo.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class BookDeletedEvent extends DomainEvent {

    public BookDeletedEvent(String aggregateId) {
        super(aggregateId);
    }

    @JsonCreator
    private BookDeletedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId
    ) {
        super(eventId, occurredAt, aggregateId);
    }
}
