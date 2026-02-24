package com.oscaruiz.mycqrs.demo.domain.model;

import com.oscaruiz.mycqrs.core.domain.event.Event;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void pullDomainEventsClearsRecordedEvents() {
        BookAggregate aggregate = BookAggregate.create("DDD", "Eric Evans");

        aggregate.pullDomainEvents();

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }
}
