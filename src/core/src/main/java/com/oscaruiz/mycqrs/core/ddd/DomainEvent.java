package com.oscaruiz.mycqrs.core.ddd;

import com.oscaruiz.mycqrs.core.domain.event.Event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for DDD domain events. Carries the standard metadata
 * (eventId, occurredAt, aggregateId) so concrete events only need to
 * declare their own payload. Enforces that every domain event is bound
 * to a non-blank aggregate identifier.
 */
public abstract class DomainEvent implements Event {

    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
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

    @Override
    public String getAggregateId() {
        return aggregateId;
    }
}
