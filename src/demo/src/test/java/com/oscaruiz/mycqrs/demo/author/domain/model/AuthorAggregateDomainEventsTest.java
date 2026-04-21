package com.oscaruiz.mycqrs.demo.author.domain.model;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorCreatedEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorDeletedEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorRenamedEvent;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorAggregateDomainEventsTest {

    @Test
    void createRecordsAuthorCreatedEventWithProvidedId() {
        String id = UUID.randomUUID().toString();
        AuthorAggregate aggregate = AuthorAggregate.create(id, "George", "Orwell", 1903);

        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        AuthorCreatedEvent event = (AuthorCreatedEvent) events.get(0);
        assertEquals(id, aggregate.getId());
        assertEquals(id, event.getAggregateId());
        assertEquals("George", event.getFirstName());
        assertEquals("Orwell", event.getLastName());
        assertEquals(1903, event.getBirthYear());
    }

    @Test
    void createAcceptsNullBirthYear() {
        String id = UUID.randomUUID().toString();
        AuthorAggregate aggregate = AuthorAggregate.create(id, "Anonymous", "Unknown", null);

        List<DomainEvent> events = aggregate.pullDomainEvents();

        assertEquals(1, events.size());
        AuthorCreatedEvent event = (AuthorCreatedEvent) events.get(0);
        assertNull(event.getBirthYear());
        assertNull(aggregate.getBirthYear());
    }

    @Test
    void createThrowsWhenIdIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create(null, "George", "Orwell", 1903));
        assertEquals("id cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenIdIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create("   ", "George", "Orwell", 1903));
        assertEquals("id cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenIdIsNotAValidUuid() {
        assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create("not-a-uuid", "George", "Orwell", 1903));
    }

    @Test
    void createThrowsWhenFirstNameIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create(UUID.randomUUID().toString(), "", "Orwell", 1903));
        assertEquals("firstName cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenLastNameIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create(UUID.randomUUID().toString(), "George", "  ", 1903));
        assertEquals("lastName cannot be null or blank", ex.getMessage());
    }

    @Test
    void createThrowsWhenBirthYearIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create(UUID.randomUUID().toString(), "George", "Orwell", 0));
        assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create(UUID.randomUUID().toString(), "George", "Orwell", -100));
    }

    @Test
    void createThrowsWhenBirthYearIsInFuture() {
        int futureYear = Year.now().getValue() + 1;
        assertThrows(IllegalArgumentException.class,
                () -> AuthorAggregate.create(UUID.randomUUID().toString(), "George", "Orwell", futureYear));
    }

    @Test
    void renameDoesNotEmitEventWhenNothingChanges() {
        String id = UUID.randomUUID().toString();
        AuthorAggregate aggregate = AuthorAggregate.rehydrate(id, "George", "Orwell", 1903, false);

        aggregate.rename("George", "Orwell");

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }

    @Test
    void renameEmitsAuthorRenamedEventWhenValuesChange() {
        String id = UUID.randomUUID().toString();
        AuthorAggregate aggregate = AuthorAggregate.rehydrate(id, "George", "Orwell", 1903, false);

        aggregate.rename("Eric", "Blair");

        List<DomainEvent> events = aggregate.pullDomainEvents();
        assertEquals(1, events.size());
        AuthorRenamedEvent event = (AuthorRenamedEvent) events.get(0);
        assertEquals(id, event.getAggregateId());
        assertEquals("Eric", event.getFirstName());
        assertEquals("Blair", event.getLastName());
        assertEquals("Eric", aggregate.getFirstName());
        assertEquals("Blair", aggregate.getLastName());
    }

    @Test
    void renameThrowsWhenFirstNameIsBlank() {
        AuthorAggregate aggregate = AuthorAggregate.rehydrate(UUID.randomUUID().toString(), "George", "Orwell", 1903, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.rename("", "Orwell"));
        assertEquals("firstName cannot be null or blank", ex.getMessage());
    }

    @Test
    void renameThrowsWhenLastNameIsBlank() {
        AuthorAggregate aggregate = AuthorAggregate.rehydrate(UUID.randomUUID().toString(), "George", "Orwell", 1903, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aggregate.rename("George", null));
        assertEquals("lastName cannot be null or blank", ex.getMessage());
    }

    @Test
    void renameThrowsWhenAggregateIsDeleted() {
        AuthorAggregate aggregate = AuthorAggregate.rehydrate(UUID.randomUUID().toString(), "George", "Orwell", 1903, true);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> aggregate.rename("Eric", "Blair"));
        assertEquals("Cannot rename a deleted author", ex.getMessage());
    }

    @Test
    void deleteRecordsAuthorDeletedEvent() {
        String id = UUID.randomUUID().toString();
        AuthorAggregate aggregate = AuthorAggregate.rehydrate(id, "George", "Orwell", 1903, false);

        aggregate.delete();

        List<DomainEvent> events = aggregate.pullDomainEvents();
        assertEquals(1, events.size());
        assertEquals(id, ((AuthorDeletedEvent) events.get(0)).getAggregateId());
        assertTrue(aggregate.isDeleted());
    }

    @Test
    void deleteTwiceThrowsIllegalStateException() {
        AuthorAggregate aggregate = AuthorAggregate.rehydrate(UUID.randomUUID().toString(), "George", "Orwell", 1903, false);

        aggregate.delete();
        IllegalStateException ex = assertThrows(IllegalStateException.class, aggregate::delete);
        assertEquals("Author is already deleted", ex.getMessage());
    }

    @Test
    void pullDomainEventsClearsRecordedEvents() {
        AuthorAggregate aggregate = AuthorAggregate.create(UUID.randomUUID().toString(), "George", "Orwell", 1903);

        aggregate.pullDomainEvents();

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }
}
