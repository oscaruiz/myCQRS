package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderCreatedEvent;

public class OrderCreatedProjectionHandler implements EventHandler<OrderCreatedEvent> {

    private final InMemoryOrderReadModel readModel;

    public OrderCreatedProjectionHandler(InMemoryOrderReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public void handle(OrderCreatedEvent event) {
        readModel.onCreated(event);
    }
}
