package com.oscaruiz.mycqrs.demo.domain.model;

import com.oscaruiz.mycqrs.core.domain.event.Event;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookDeletedEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;

import java.util.ArrayList;
import java.util.List;

public class BookAggregate {

    private Long id;
    private String title;
    private String author;
    private boolean deleted;
    private final List<Event> domainEvents = new ArrayList<>();

    private BookAggregate(Long id, String title, String author, boolean deleted) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.deleted = deleted;
    }

    public static BookAggregate create(String title, String author) {

        requireNonBlank(title, "title");
        requireNonBlank(author, "author");

        BookAggregate aggregate = new BookAggregate(null, title, author, false);
        aggregate.recordEvent(new BookCreatedEvent(null, title, author));

        return aggregate;
    }

    public static BookAggregate rehydrate(Long id, String title, String author, boolean deleted) {
        return new BookAggregate(id, title, author, deleted);
    }

    public void assignId(Long id) {
        this.id = id;
        backfillAggregateIdInDomainEvents();
    }

    public void updateIfPresent(String title, String author) {
        if (deleted) {
            throw new IllegalStateException("Cannot update a deleted book");
        }

        requireNonBlank(title, "title");
        requireNonBlank(author, "author");

        if (this.title.equals(title) && this.author.equals(author)) {
            return;
        }

        this.title = title;
        this.author = author;

        if (id == null) {
            throw new IllegalStateException("Cannot emit update event without aggregate id");
        }

        recordEvent(new BookUpdatedEvent(String.valueOf(id), this.title, this.author));
    }

    public void delete() {
        if (id == null) {
            throw new IllegalStateException("Cannot delete a book without aggregate id");
        }
        if (deleted) {
            throw new IllegalStateException("Book is already deleted");
        }

        deleted = true;
        recordEvent(new BookDeletedEvent(String.valueOf(id)));
    }

    protected void recordEvent(Event event) {
        domainEvents.add(event);
    }

    public List<Event> pullDomainEvents() {
        List<Event> events = new ArrayList<>(domainEvents);

        for (Event event : events) {
            String aggregateId = event.getAggregateId();

            if (aggregateId == null || aggregateId.isBlank()) {
                throw new IllegalStateException(
                        "Domain event " + event.getClass().getSimpleName() +
                                " has no aggregateId"
                );
            }
        }

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

    public boolean isDeleted() {
        return deleted;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }
}
