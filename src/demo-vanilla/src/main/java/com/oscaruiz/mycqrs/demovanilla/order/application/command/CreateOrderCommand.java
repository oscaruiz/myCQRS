package com.oscaruiz.mycqrs.demovanilla.order.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;

import java.util.UUID;

public record CreateOrderCommand(UUID commandId, UUID orderId, String description) implements Command {}
