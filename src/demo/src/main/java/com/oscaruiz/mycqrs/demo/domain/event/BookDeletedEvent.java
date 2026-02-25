package com.oscaruiz.mycqrs.demo.domain.event;

import com.oscaruiz.mycqrs.core.domain.event.Event;

public class BookDeletedEvent implements Event {

    private final String aggregateId;

    public BookDeletedEvent(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateId() {
        return aggregateId;
    }
}
