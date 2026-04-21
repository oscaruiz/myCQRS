package com.oscaruiz.mycqrs.demo.author.infrastructure.outbox.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class AuthorRenamedEventMixin {

    @JsonCreator
    public AuthorRenamedEventMixin(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName
    ) {}
}
