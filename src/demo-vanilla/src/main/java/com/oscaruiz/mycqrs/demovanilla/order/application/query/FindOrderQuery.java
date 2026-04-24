package com.oscaruiz.mycqrs.demovanilla.order.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.Query;

import java.util.Optional;
import java.util.UUID;

public record FindOrderQuery(UUID orderId) implements Query<Optional<OrderResponse>> {}
