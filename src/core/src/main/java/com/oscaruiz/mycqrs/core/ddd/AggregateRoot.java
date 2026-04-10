package com.oscaruiz.mycqrs.core.ddd;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot<ID> {
    private final List<DomainEvent> events = new ArrayList<>();

    protected void recordEvent(DomainEvent e) {
        events.add(e);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public abstract ID getId();
}
