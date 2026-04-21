package com.oscaruiz.mycqrs.demo.book.domain.model;

import com.oscaruiz.mycqrs.core.ddd.AggregateRoot;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorAddedToBookEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorRemovedFromBookEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookDeletedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookUpdatedEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BookAggregate extends AggregateRoot<String> {

    private final String id;
    private String title;
    private boolean deleted;
    private final Set<String> authorIds;

    private BookAggregate(String id, String title, boolean deleted, Set<String> authorIds) {
        this.id = id;
        this.title = title;
        this.deleted = deleted;
        this.authorIds = new HashSet<>(authorIds);
    }

    public static BookAggregate create(String id, String title) {

        requireNonBlank(id, "id");
        requireNonBlank(title, "title");
        UUID.fromString(id);

        BookAggregate aggregate = new BookAggregate(id, title, false, new HashSet<>());
        aggregate.recordEvent(new BookCreatedEvent(id, title));

        return aggregate;
    }

    public static BookAggregate rehydrate(String id, String title, boolean deleted, Set<String> authorIds) {
        return new BookAggregate(id, title, deleted, authorIds);
    }

    public void update(String title) {
        if (deleted) {
            throw new IllegalStateException("Cannot update a deleted book");
        }

        requireNonBlank(title, "title");

        if (this.title.equals(title)) {
            return;
        }

        this.title = title;
        recordEvent(new BookUpdatedEvent(id, this.title));
    }

    public void delete() {
        if (deleted) {
            throw new IllegalStateException("Book is already deleted");
        }

        deleted = true;
        recordEvent(new BookDeletedEvent(id));
    }

    public void addAuthor(String authorId) {
        if (deleted) {
            throw new IllegalStateException("Cannot modify authors of a deleted book");
        }
        requireNonBlank(authorId, "authorId");
        UUID.fromString(authorId);

        if (!authorIds.add(authorId)) {
            return;
        }
        recordEvent(new AuthorAddedToBookEvent(id, authorId));
    }

    public void removeAuthor(String authorId) {
        if (deleted) {
            throw new IllegalStateException("Cannot modify authors of a deleted book");
        }
        requireNonBlank(authorId, "authorId");

        if (!authorIds.remove(authorId)) {
            return;
        }
        recordEvent(new AuthorRemovedFromBookEvent(id, authorId));
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Set<String> getAuthorIds() {
        return Collections.unmodifiableSet(authorIds);
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }
}
