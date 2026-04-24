package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderConfirmedProjectionHandler implements EventHandler<OrderConfirmedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedProjectionHandler.class);

    private final InMemoryOrderReadModel readModel;

    public OrderConfirmedProjectionHandler(InMemoryOrderReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public void handle(OrderConfirmedEvent event) {
        log.debug("PROJECTION: OrderConfirmedProjectionHandler updating read model (status=CONFIRMED)");
        readModel.onConfirmed(event);
    }
}
