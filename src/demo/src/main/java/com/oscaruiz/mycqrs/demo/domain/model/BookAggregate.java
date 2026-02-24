package com.oscaruiz.mycqrs.demo.domain.model;

import com.oscaruiz.mycqrs.core.domain.event.Event;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;

import java.util.ArrayList;
import java.util.List;

public class BookAggregate {

    private Long id;
    private String title;
    private String author;
    private final List<Event> domainEvents = new ArrayList<>();

    public BookAggregate(Long id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public static BookAggregate create(String title, String author) {
        BookAggregate aggregate = new BookAggregate(null, title, author);
        aggregate.recordEvent(new BookCreatedEvent(null, title, author));
        return aggregate;
    }

    public static BookAggregate rehydrate(Long id, String title, String author) {
        return new BookAggregate(id, title, author);
    }

    public void assignId(Long id) {
        this.id = id;
        backfillAggregateIdInDomainEvents();
    }

    public void updateIfPresent(String title, String author) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (author != null && !author.isBlank()) {
            this.author = author;
        }
    }

    protected void recordEvent(Event event) {
        domainEvents.add(event);
    }

    public List<Event> pullDomainEvents() {
        List<Event> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void backfillAggregateIdInDomainEvents() {
        for (int index = 0; index < domainEvents.size(); index++) {
            Event event = domainEvents.get(index);
            if (event instanceof BookCreatedEvent createdEvent
                    && (createdEvent.getAggregateId() == null || createdEvent.getAggregateId().isBlank())) {
                domainEvents.set(index, new BookCreatedEvent(String.valueOf(id), createdEvent.getTitle(), createdEvent.getAuthor()));
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
