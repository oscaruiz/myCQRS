package com.oscaruiz.mycqrs.core.ddd;

import com.oscaruiz.mycqrs.core.contracts.event.Event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for DDD domain events. Carries the standard metadata
 * (eventId, occurredAt, aggregateId) so concrete events only need to
 * declare their own payload. Enforces that every domain event is bound
 * to a non-blank aggregate identifier.
 */
public abstract class DomainEvent implements Event {

    private String eventId;
    private Instant occurredAt;
    private String aggregateId;

    /**
     * No-arg constructor for Jackson deserialization (outbox poller).
     */
    protected DomainEvent() {
    }

    protected DomainEvent(String aggregateId) {
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId cannot be null or blank");
        }
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.aggregateId = aggregateId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }
}
