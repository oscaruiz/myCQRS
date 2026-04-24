package com.oscaruiz.mycqrs.demovanilla.order.infrastructure.http;

import java.util.UUID;

public record CreateOrderRequest(UUID orderId, String description) {}
