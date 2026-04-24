package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderConfirmedEvent;

public class OrderConfirmedProjectionHandler implements EventHandler<OrderConfirmedEvent> {

    private final InMemoryOrderReadModel readModel;

    public OrderConfirmedProjectionHandler(InMemoryOrderReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public void handle(OrderConfirmedEvent event) {
        readModel.onConfirmed(event);
    }
}
