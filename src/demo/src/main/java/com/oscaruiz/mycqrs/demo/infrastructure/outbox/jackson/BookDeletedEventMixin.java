package com.oscaruiz.mycqrs.demo.infrastructure.outbox.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class BookDeletedEventMixin {

    @JsonCreator
    public BookDeletedEventMixin(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId
    ) {}
}
