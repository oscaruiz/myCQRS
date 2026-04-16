package com.oscaruiz.mycqrs.demo.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;

public class BookDeletedEvent extends DomainEvent {

    protected BookDeletedEvent() {
    }

    public BookDeletedEvent(String aggregateId) {
        super(aggregateId);
    }
}
