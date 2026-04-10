package com.oscaruiz.mycqrs.core.ddd;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregateRootTest {

    @Test
    void pullDomainEvents_returnsEmptyList_whenNoEventsRecorded() {
        TestAggregate aggregate = new TestAggregate(1L);

        List<DomainEvent> pulled = aggregate.pullDomainEvents();

        assertTrue(pulled.isEmpty());
    }

    @Test
    void pullDomainEvents_returnsAllRecordedEvents_inOrder() {
        TestAggregate aggregate = new TestAggregate(1L);
        TestEvent first = new TestEvent("a");
        TestEvent second = new TestEvent("b");
        TestEvent third = new TestEvent("c");
        aggregate.emit(first);
        aggregate.emit(second);
        aggregate.emit(third);

        List<DomainEvent> pulled = aggregate.pullDomainEvents();

        assertEquals(3, pulled.size());
        assertSame(first, pulled.get(0));
        assertSame(second, pulled.get(1));
        assertSame(third, pulled.get(2));
    }

    @Test
    void pullDomainEvents_clearsBuffer_soSecondCallReturnsEmpty() {
        TestAggregate aggregate = new TestAggregate(1L);
        aggregate.emit(new TestEvent("a"));
        aggregate.emit(new TestEvent("b"));
        aggregate.pullDomainEvents();

        List<DomainEvent> pulled = aggregate.pullDomainEvents();

        assertTrue(pulled.isEmpty());
    }

    @Test
    void pullDomainEvents_returnsImmutableList() {
        TestAggregate aggregate = new TestAggregate(1L);
        aggregate.emit(new TestEvent("a"));

        List<DomainEvent> pulled = aggregate.pullDomainEvents();

        assertThrows(UnsupportedOperationException.class,
                () -> pulled.add(new TestEvent("x")));
    }

    private static final class TestEvent extends DomainEvent {
        TestEvent(String aggregateId) {
            super(aggregateId);
        }
    }

    private static final class TestAggregate extends AggregateRoot<Long> {
        private final Long id;

        TestAggregate(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }

        void emit(DomainEvent e) {
            recordEvent(e);
        }
    }
}
