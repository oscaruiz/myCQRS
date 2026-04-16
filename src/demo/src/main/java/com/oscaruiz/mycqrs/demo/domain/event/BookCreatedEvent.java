package com.oscaruiz.mycqrs.demo.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

import java.time.Instant;

public class BookCreatedEvent extends DomainEvent {
    private final String title;
    private final String author;

    public BookCreatedEvent(String aggregateId, String title, String author) {
        super(aggregateId);
        this.title = title;
        this.author = author;
    }

    @JsonCreator
    private BookCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author
    ) {
        super(eventId, occurredAt, aggregateId);
        this.title = title;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

}
