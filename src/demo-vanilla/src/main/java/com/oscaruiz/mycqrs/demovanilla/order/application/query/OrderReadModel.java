package com.oscaruiz.mycqrs.demovanilla.order.application.query;

import java.util.Optional;
import java.util.UUID;

public interface OrderReadModel {
    Optional<OrderResponse> findById(UUID id);
}
