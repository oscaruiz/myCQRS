package com.oscaruiz.mycqrs.demovanilla.order.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderAggregate;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderId;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderRepository;

public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand> {
    private final OrderRepository orderRepository;
    private final EventBus eventBus;

    public CreateOrderCommandHandler(OrderRepository orderRepository, EventBus eventBus) {
        this.orderRepository = orderRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(CreateOrderCommand command) {
        var order = OrderAggregate.create(
            OrderId.of(command.orderId()),
            command.description()
        );
        orderRepository.save(order);
        order.pullDomainEvents().forEach(eventBus::publish);
    }
}
