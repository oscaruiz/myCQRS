package com.oscaruiz.mycqrs.demovanilla.order.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderId;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderRepository;

public class ConfirmOrderCommandHandler implements CommandHandler<ConfirmOrderCommand> {
    private final OrderRepository orderRepository;
    private final EventBus eventBus;

    public ConfirmOrderCommandHandler(OrderRepository orderRepository, EventBus eventBus) {
        this.orderRepository = orderRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(ConfirmOrderCommand command) {
        var orderId = OrderId.of(command.orderId());
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId.value()));
        order.confirm();
        orderRepository.save(order);
        order.pullDomainEvents().forEach(eventBus::publish);
    }
}
