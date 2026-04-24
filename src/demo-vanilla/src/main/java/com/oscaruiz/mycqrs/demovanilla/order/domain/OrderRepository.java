package com.oscaruiz.mycqrs.demovanilla.order.domain;

import java.util.Optional;

public interface OrderRepository {
    void save(OrderAggregate order);
    Optional<OrderAggregate> findById(OrderId id);
}
