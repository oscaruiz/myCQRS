package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.http;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.ConfirmOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.CreateOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.FindOrderQuery;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.OrderResponse;
import io.javalin.Javalin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OrderHttpAdapter {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public OrderHttpAdapter(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    public void setupRoutes(Javalin app) {
        app.post("/orders", ctx -> {
            var req = ctx.bodyAsClass(CreateOrderRequest.class);
            var orderId = req.orderId() != null ? req.orderId() : UUID.randomUUID();
            commandBus.send(new CreateOrderCommand(UUID.randomUUID(), orderId, req.description()));
            ctx.status(201).json(Map.of("orderId", orderId));
        });

        app.patch("/orders/{id}/confirm", ctx -> {
            var orderId = UUID.fromString(ctx.pathParam("id"));
            commandBus.send(new ConfirmOrderCommand(UUID.randomUUID(), orderId));
            ctx.status(200);
        });

        app.get("/orders/{id}", ctx -> {
            var orderId = UUID.fromString(ctx.pathParam("id"));
            Optional<OrderResponse> result = queryBus.handle(new FindOrderQuery(orderId));
            result.ifPresentOrElse(
                ctx::json,
                () -> ctx.status(404)
            );
        });
    }
}
