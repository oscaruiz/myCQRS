package com.oscaruiz.mycqrs.demo.domain.event;

import com.oscaruiz.mycqrs.core.domain.event.Event;

public class BookUpdatedEvent implements Event {

    private final String aggregateId;
    private final String title;
    private final String author;

    public BookUpdatedEvent(String aggregateId, String title, String author) {
        this.aggregateId = aggregateId;
        this.title = title;
        this.author = author;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
