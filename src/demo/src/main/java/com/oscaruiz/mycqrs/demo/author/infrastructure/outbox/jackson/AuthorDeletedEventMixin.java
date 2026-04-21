package com.oscaruiz.mycqrs.demo.author.infrastructure.outbox.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class AuthorDeletedEventMixin {

    @JsonCreator
    public AuthorDeletedEventMixin(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId
    ) {}
}
