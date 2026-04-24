package com.oscaruiz.mycqrs.demovanilla.order.domain.event;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderId;

public class OrderConfirmedEvent extends DomainEvent {
    private final OrderId orderId;

    public OrderConfirmedEvent(OrderId orderId) {
        super(orderId.value().toString());
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
