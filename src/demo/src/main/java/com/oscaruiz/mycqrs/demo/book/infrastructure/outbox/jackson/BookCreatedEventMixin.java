package com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class BookCreatedEventMixin {

    @JsonCreator
    public BookCreatedEventMixin(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author
    ) {}
}
