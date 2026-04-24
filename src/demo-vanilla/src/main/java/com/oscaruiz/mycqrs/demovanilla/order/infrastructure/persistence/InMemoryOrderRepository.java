package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.persistence;

import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderAggregate;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderId;
import com.oscaruiz.mycqrs.demovanilla.order.domain.OrderRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentHashMap<OrderId, OrderAggregate> store = new ConcurrentHashMap<>();

    @Override
    public void save(OrderAggregate order) {
        store.put(order.id(), order);
    }

    @Override
    public Optional<OrderAggregate> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }
}
