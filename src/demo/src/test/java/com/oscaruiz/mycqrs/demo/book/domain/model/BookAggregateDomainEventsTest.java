package com.oscaruiz.mycqrs.demo.book.domain.model;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorAddedToBookEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorRemovedFromBookEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookDeletedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookUpdatedEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookAggregateDomainEventsTest {

    @Test
    void createRecordsBookCreatedEventWithProvidedId() {
        String id = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.create(id, "Clean Code");

        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookCreatedEvent event = (BookCreatedEvent) events.get(0);
        assertEquals(id, aggregate.getId());
        assertEquals(id, event.getAggregateId());
        assertEquals("Clean Code", event.getTitle());
        // Original assertion removed: event.getAuthor() no longer exists.
    }

    @Test
    void updateRecordsBookUpdatedEventOnlyWhenStateChanges() {
        String id = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.rehydrate(id, "Old", false, Set.of());

        aggregate.update("Old");
        assertTrue(aggregate.pullDomainEvents().isEmpty());

        aggregate.update("New");
        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookUpdatedEvent event = (BookUpdatedEvent) events.get(0);
        assertEquals(id, event.getAggregateId());
        assertEquals("New", event.getTitle());
    }

    @Test
    void updateThrowsWhenTitleIsNull() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", false, Set.of());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update(null));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenTitleIsBlank() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", false, Set.of());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update(""));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenAggregateIsDeleted() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", true, Set.of());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> aggregate.update("New Title"));
        assertEquals("Cannot update a deleted book", ex.getMessage());
    }

    @Test
    void deleteRecordsBookDeletedEventAndPreventsSecondDelete() {
        String id = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.rehydrate(id, "Book", false, Set.of());

        aggregate.delete();
        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        assertEquals(id, ((BookDeletedEvent) events.get(0)).getAggregateId());
        assertThrows(IllegalStateException.class, aggregate::delete);
    }

    @Test
    void createThrowsWhenIdIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create(null, "Title"));
        assertEquals("id cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenIdIsNotAValidUuid() {
        assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create("pepito", "Title"));
    }

    @Test
    void createThrowsWhenTitleIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create(UUID.randomUUID().toString(), null));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenTitleIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create(UUID.randomUUID().toString(), "  "));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void pullDomainEventsClearsRecordedEvents() {
        BookAggregate aggregate = BookAggregate.create(UUID.randomUUID().toString(), "DDD");

        aggregate.pullDomainEvents();

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }

    @Test
    void addAuthorAppendsAndEmitsEvent() {
        String bookId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.create(bookId, "Book");
        aggregate.pullDomainEvents();

        aggregate.addAuthor(authorId);

        List<DomainEvent> events = aggregate.pullDomainEvents();
        assertEquals(1, events.size());
        AuthorAddedToBookEvent event = (AuthorAddedToBookEvent) events.get(0);
        assertEquals(bookId, event.getAggregateId());
        assertEquals(authorId, event.getAuthorId());
        assertTrue(aggregate.getAuthorIds().contains(authorId));
    }

    @Test
    void addSameAuthorTwiceIsNoOp() {
        String bookId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.create(bookId, "Book");
        aggregate.pullDomainEvents();

        aggregate.addAuthor(authorId);
        aggregate.pullDomainEvents();

        aggregate.addAuthor(authorId);

        assertTrue(aggregate.pullDomainEvents().isEmpty());
        assertEquals(1, aggregate.getAuthorIds().size());
    }

    @Test
    void addAuthorOnDeletedBookThrows() {
        BookAggregate aggregate = BookAggregate.rehydrate(
                UUID.randomUUID().toString(), "Dead", true, Set.of());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> aggregate.addAuthor(UUID.randomUUID().toString()));
        assertEquals("Cannot modify authors of a deleted book", ex.getMessage());
    }

    @Test
    void addWithInvalidAuthorIdThrows() {
        BookAggregate aggregate = BookAggregate.rehydrate(
                UUID.randomUUID().toString(), "Book", false, Set.of());
        assertThrows(IllegalArgumentException.class, () -> aggregate.addAuthor(null));
        assertThrows(IllegalArgumentException.class, () -> aggregate.addAuthor("  "));
        assertThrows(IllegalArgumentException.class, () -> aggregate.addAuthor("not-a-uuid"));
    }

    @Test
    void removeAuthorEmitsEvent() {
        String bookId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.rehydrate(bookId, "Book", false, Set.of(authorId));

        aggregate.removeAuthor(authorId);

        List<DomainEvent> events = aggregate.pullDomainEvents();
        assertEquals(1, events.size());
        AuthorRemovedFromBookEvent event = (AuthorRemovedFromBookEvent) events.get(0);
        assertEquals(bookId, event.getAggregateId());
        assertEquals(authorId, event.getAuthorId());
        assertTrue(aggregate.getAuthorIds().isEmpty());
    }

    @Test
    void removeNonexistentAuthorIsNoOp() {
        BookAggregate aggregate = BookAggregate.rehydrate(
                UUID.randomUUID().toString(), "Book", false, Set.of());

        aggregate.removeAuthor(UUID.randomUUID().toString());

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }
}
