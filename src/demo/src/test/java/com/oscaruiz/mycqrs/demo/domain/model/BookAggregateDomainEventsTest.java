package com.oscaruiz.mycqrs.demo.domain.model;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookDeletedEvent;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookAggregateDomainEventsTest {

    @Test
    void createRecordsBookCreatedEventAndAssignIdBackfillsAggregateId() {
        BookAggregate aggregate = BookAggregate.create("Clean Code", "Robert C. Martin");
        aggregate.assignId(26L);

        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookCreatedEvent event = (BookCreatedEvent) events.get(0);
        assertEquals("26", event.getAggregateId());
        assertEquals("Clean Code", event.getTitle());
        assertEquals("Robert C. Martin", event.getAuthor());
    }

    @Test
    void updateRecordsBookUpdatedEventOnlyWhenStateChanges() {
        BookAggregate aggregate = BookAggregate.rehydrate(10L, "Old", "Author", false);

        aggregate.update("Old", "Author");
        assertTrue(aggregate.pullDomainEvents().isEmpty());

        aggregate.update("New", "Author");
        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookUpdatedEvent event = (BookUpdatedEvent) events.get(0);
        assertEquals("10", event.getAggregateId());
        assertEquals("New", event.getTitle());
    }

    @Test
    void updateThrowsWhenTitleIsNull() {
        BookAggregate aggregate = BookAggregate.rehydrate(1L, "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update(null, "Author"));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenTitleIsBlank() {
        BookAggregate aggregate = BookAggregate.rehydrate(1L, "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update("", "Author"));
        assertEquals("title cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenAuthorIsNull() {
        BookAggregate aggregate = BookAggregate.rehydrate(1L, "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update("Title", null));
        assertEquals("author cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenAuthorIsBlank() {
        BookAggregate aggregate = BookAggregate.rehydrate(1L, "Title", "Author", false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.update("Title", ""));
        assertEquals("author cannot be null or blank", ex.getMessage());
    }

    @Test
    void updateThrowsWhenAggregateIsDeleted() {
        BookAggregate aggregate = BookAggregate.rehydrate(1L, "Title", "Author", true);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> aggregate.update("New Title", "New Author"));
        assertEquals("Cannot update a deleted book", ex.getMessage());
    }

    @Test
    void deleteRecordsBookDeletedEventAndPreventsSecondDelete() {
        BookAggregate aggregate = BookAggregate.rehydrate(15L, "Book", "Author", false);

        aggregate.delete();
        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        assertEquals("15", ((BookDeletedEvent) events.get(0)).getAggregateId());
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
        aggregate.assignId(1L);

        aggregate.pullDomainEvents();

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }
}
