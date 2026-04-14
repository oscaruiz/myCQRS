package com.oscaruiz.mycqrs.demo.domain.model;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookDeletedEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookAggregateDomainEventsTest {

    @Test
    void createRecordsBookCreatedEventWithValidUuidAggregateId() {
        BookAggregate aggregate = BookAggregate.create("Clean Code", "Robert C. Martin");

        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookCreatedEvent event = (BookCreatedEvent) events.get(0);
        assertDoesNotThrow(() -> UUID.fromString(event.getAggregateId()));
        assertEquals(aggregate.getId(), event.getAggregateId());
        assertEquals("Clean Code", event.getTitle());
        assertEquals("Robert C. Martin", event.getAuthor());
    }

    @Test
    void updateRecordsBookUpdatedEventOnlyWhenStateChanges() {
        String id = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.rehydrate(id, "Old", "Author", false);

        aggregate.update("Old", "Author");
        assertTrue(aggregate.pullDomainEvents().isEmpty());

        aggregate.update("New", "Author");
        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookUpdatedEvent event = (BookUpdatedEvent) events.get(0);
        assertEquals(id, event.getAggregateId());
        assertEquals("New", event.getTitle());
    }

    @Test
    void updateThrowsWhenTitleIsNull() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update(null, "Author"));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenTitleIsBlank() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update("", "Author"));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenAuthorIsNull() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update("Title", null));
        assertEquals("author cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenAuthorIsBlank() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update("Title", ""));
        assertEquals("author cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenAggregateIsDeleted() {
        BookAggregate aggregate = BookAggregate.rehydrate(UUID.randomUUID().toString(), "Title", "Author", true);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> aggregate.update("New Title", "New Author"));
        assertEquals("Cannot update a deleted book", ex.getMessage());
    }

    @Test
    void deleteRecordsBookDeletedEventAndPreventsSecondDelete() {
        String id = UUID.randomUUID().toString();
        BookAggregate aggregate = BookAggregate.rehydrate(id, "Book", "Author", false);

        aggregate.delete();
        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        assertEquals(id, ((BookDeletedEvent) events.get(0)).getAggregateId());
        assertThrows(IllegalStateException.class, aggregate::delete);
    }

    @Test
    void createThrowsWhenTitleIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create(null, "Author"));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenTitleIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create("  ", "Author"));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenAuthorIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create("Title", null));
        assertEquals("author cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenAuthorIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookAggregate.create("Title", ""));
        assertEquals("author cannot be null or blank", ex.getMessage());
    }

    @Test
    void pullDomainEventsClearsRecordedEvents() {
        BookAggregate aggregate = BookAggregate.create("DDD", "Eric Evans");

        aggregate.pullDomainEvents();

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }
}
