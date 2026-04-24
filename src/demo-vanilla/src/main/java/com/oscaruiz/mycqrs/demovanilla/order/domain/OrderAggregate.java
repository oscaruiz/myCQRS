package com.oscaruiz.mycqrs.demovanilla.order.domain;

import com.oscaruiz.mycqrs.core.ddd.AggregateRoot;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderConfirmedEvent;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderCreatedEvent;

public class OrderAggregate extends AggregateRoot<OrderId> {
    private final OrderId id;
    private String description;
    private OrderStatus status;

    private OrderAggregate(OrderId id, String description, OrderStatus status) {
        this.id = id;
        this.description = description;
        this.status = status;
    }

    public static OrderAggregate create(OrderId id, String description) {
        var order = new OrderAggregate(id, description, OrderStatus.PENDING);
        order.recordEvent(new OrderCreatedEvent(id, description));
        return order;
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
        recordEvent(new OrderConfirmedEvent(id));
    }

    @Override
    public OrderId getId() {
        return id;
    }

    public OrderId id() {
        return id;
    }

    public String description() {
        return description;
    }

    public OrderStatus status() {
        return status;
    }
}
