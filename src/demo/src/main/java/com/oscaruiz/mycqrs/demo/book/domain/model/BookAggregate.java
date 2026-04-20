package com.oscaruiz.mycqrs.demo.book.domain.model;

import com.oscaruiz.mycqrs.core.ddd.AggregateRoot;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookDeletedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookUpdatedEvent;

import java.util.UUID;

public class BookAggregate extends AggregateRoot<String> {

    private final String id;
    private String title;
    private String author;
    private boolean deleted;

    private BookAggregate(String id, String title, String author, boolean deleted) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.deleted = deleted;
    }

    public static BookAggregate create(String id, String title, String author) {

        requireNonBlank(id, "id");
        requireNonBlank(title, "title");
        requireNonBlank(author, "author");
        UUID.fromString(id);

        BookAggregate aggregate = new BookAggregate(id, title, author, false);
        aggregate.recordEvent(new BookCreatedEvent(id, title, author));

        return aggregate;
    }

    public static BookAggregate rehydrate(String id, String title, String author, boolean deleted) {
        return new BookAggregate(id, title, author, deleted);
    }

    public void update(String title, String author) {
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

        recordEvent(new BookUpdatedEvent(id, this.title, this.author));
    }

    public void delete() {
        if (deleted) {
            throw new IllegalStateException("Book is already deleted");
        }

        deleted = true;
        recordEvent(new BookDeletedEvent(id));
    }

    @Override
    public String getId() {
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
