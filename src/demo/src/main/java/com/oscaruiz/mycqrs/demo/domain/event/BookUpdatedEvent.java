package com.oscaruiz.mycqrs.demo.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

public class BookUpdatedEvent extends DomainEvent {

    private final String title;
    private final String author;

    public BookUpdatedEvent(String aggregateId, String title, String author) {
        super(aggregateId);
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
