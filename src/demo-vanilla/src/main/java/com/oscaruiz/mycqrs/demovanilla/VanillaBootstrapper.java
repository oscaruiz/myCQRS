package com.oscaruiz.mycqrs.demovanilla;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.query.SimpleQueryBus;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.ConfirmOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.ConfirmOrderCommandHandler;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.CreateOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.CreateOrderCommandHandler;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.FindOrderQuery;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.FindOrderQueryHandler;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderRepository;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderConfirmedEvent;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderCreatedEvent;
import com.oscaruiz.mycqrs.demovanilla.order.infrastructure.http.OrderHttpAdapter;
import com.oscaruiz.mycqrs.demovanilla.order.infrastructure.persistence.InMemoryOrderRepository;
import com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection.InMemoryOrderReadModel;
import com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection.OrderConfirmedProjectionHandler;
import com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection.OrderCreatedProjectionHandler;
import io.javalin.Javalin;

public class VanillaBootstrapper {
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final EventBus eventBus;
    private final OrderRepository orderRepository;
    private final InMemoryOrderReadModel readModel;

    public VanillaBootstrapper() {
        this.commandBus = new SimpleCommandBus();
        this.queryBus = new SimpleQueryBus();
        this.eventBus = new SimpleEventBus();
        this.orderRepository = new InMemoryOrderRepository();
        this.readModel = new InMemoryOrderReadModel();

        registerCommandHandlers();
        registerQueryHandlers();
        registerEventHandlers();
    }

    private void registerCommandHandlers() {
        commandBus.registerHandler(CreateOrderCommand.class,
            new CreateOrderCommandHandler(orderRepository, eventBus));
        commandBus.registerHandler(ConfirmOrderCommand.class,
            new ConfirmOrderCommandHandler(orderRepository, eventBus));
    }

    private void registerQueryHandlers() {
        queryBus.registerHandler(FindOrderQuery.class,
            new FindOrderQueryHandler(readModel));
    }

    private void registerEventHandlers() {
        eventBus.registerHandler(OrderCreatedEvent.class,
            new OrderCreatedProjectionHandler(readModel));
        eventBus.registerHandler(OrderConfirmedEvent.class,
            new OrderConfirmedProjectionHandler(readModel));
    }

    public CommandBus commandBus() { return commandBus; }
    public QueryBus queryBus() { return queryBus; }
    public EventBus eventBus() { return eventBus; }

    public Javalin createHttpApp() {
        var app = Javalin.create();
        new OrderHttpAdapter(commandBus, queryBus).setupRoutes(app);
        return app;
    }
}
