package com.oscaruiz.mycqrs.demovanilla.order.application.query;

import java.util.UUID;

public record OrderResponse(UUID id, String description, String status) {}
