package com.oscaruiz.mycqrs.core.ddd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oscaruiz.mycqrs.core.contracts.event.Event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for DDD domain events. Carries the standard metadata
 * (eventId, occurredAt, aggregateId) so concrete events only need to
 * declare their own payload. Enforces that every domain event is bound
 * to a non-blank aggregate identifier.
 *
 * Immutability: all fields are final. Jackson deserialization uses the
 * three-arg {@link JsonCreator} constructor; domain code uses the
 * single-arg convenience constructor that auto-generates eventId and
 * occurredAt.
 */
public abstract class DomainEvent implements Event {

    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    @JsonCreator
    protected DomainEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId
    ) {
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId cannot be null or blank");
        }
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.aggregateId = aggregateId;
    }

    protected DomainEvent(String aggregateId) {
        this(null, null, aggregateId);
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
