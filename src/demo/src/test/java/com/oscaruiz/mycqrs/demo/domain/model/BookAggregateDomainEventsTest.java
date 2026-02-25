package com.oscaruiz.mycqrs.demo.domain.model;

import com.oscaruiz.mycqrs.core.domain.event.Event;
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

        List<Event> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookCreatedEvent event = (BookCreatedEvent) events.get(0);
        assertEquals("26", event.getAggregateId());
        assertEquals("Clean Code", event.getTitle());
        assertEquals("Robert C. Martin", event.getAuthor());
    }

    @Test
    void updateRecordsBookUpdatedEventOnlyWhenStateChanges() {
        BookAggregate aggregate = BookAggregate.rehydrate(10L, "Old", "Author", false);

        aggregate.updateIfPresent("Old", "Author");
        assertTrue(aggregate.pullDomainEvents().isEmpty());

        aggregate.updateIfPresent("New", null);
        List<Event> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        BookUpdatedEvent event = (BookUpdatedEvent) events.get(0);
        assertEquals("10", event.getAggregateId());
        assertEquals("New", event.getTitle());
    }

    @Test
    void deleteRecordsBookDeletedEventAndPreventsSecondDelete() {
        BookAggregate aggregate = BookAggregate.rehydrate(15L, "Book", "Author", false);

        aggregate.delete();
        List<Event> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        assertEquals("15", ((BookDeletedEvent) events.get(0)).getAggregateId());
        assertThrows(IllegalStateException.class, aggregate::delete);
    }

    @Test
    void pullDomainEventsClearsRecordedEvents() {
        BookAggregate aggregate = BookAggregate.create("DDD", "Eric Evans");

        aggregate.pullDomainEvents();

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }
}
