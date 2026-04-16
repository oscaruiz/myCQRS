package com.oscaruiz.mycqrs.demo.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

public class BookCreatedEvent extends DomainEvent {
    private String title;
    private String author;

    protected BookCreatedEvent() {
    }

    public BookCreatedEvent(String aggregateId, String title, String author) {
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
