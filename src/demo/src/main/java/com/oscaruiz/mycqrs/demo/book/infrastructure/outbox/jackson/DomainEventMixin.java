package com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class DomainEventMixin {

    @JsonCreator
    public DomainEventMixin(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId
    ) {}
}
