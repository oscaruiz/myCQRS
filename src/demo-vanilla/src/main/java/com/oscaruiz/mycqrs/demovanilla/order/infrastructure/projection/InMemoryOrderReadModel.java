package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.projection;

import com.oscaruiz.mycqrs.demovanilla.order.application.query.OrderReadModel;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.OrderResponse;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderConfirmedEvent;
import com.oscaruiz.mycqrs.demovanilla.order.domain.event.OrderCreatedEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderReadModel implements OrderReadModel {
    private final ConcurrentHashMap<UUID, OrderResponse> projections = new ConcurrentHashMap<>();

    public void onCreated(OrderCreatedEvent event) {
        projections.put(
            event.orderId().value(),
            new OrderResponse(event.orderId().value(), event.description(), "PENDING")
        );
    }

    public void onConfirmed(OrderConfirmedEvent event) {
        projections.computeIfPresent(event.orderId().value(),
            (id, existing) -> new OrderResponse(existing.id(), existing.description(), "CONFIRMED"));
    }

    @Override
    public Optional<OrderResponse> findById(UUID id) {
        return Optional.ofNullable(projections.get(id));
    }
}
