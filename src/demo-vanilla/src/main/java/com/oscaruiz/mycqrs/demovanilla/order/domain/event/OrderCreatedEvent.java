package com.oscaruiz.mycqrs.demovanilla.order.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderId;

public class OrderCreatedEvent extends DomainEvent {
    private final OrderId orderId;
    private final String description;

    public OrderCreatedEvent(OrderId orderId, String description) {
        super(orderId.value().toString());
        this.orderId = orderId;
        this.description = description;
    }

    public OrderId orderId() {
        return orderId;
    }

    public String description() {
        return description;
    }
}
