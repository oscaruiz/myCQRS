package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderCreatedProjectionHandler implements EventHandler<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedProjectionHandler.class);

    private final InMemoryOrderReadModel readModel;

    public OrderCreatedProjectionHandler(InMemoryOrderReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public void handle(OrderCreatedEvent event) {
        log.debug("PROJECTION: OrderCreatedProjectionHandler updating read model (status=PENDING)");
        readModel.onCreated(event);
    }
}
