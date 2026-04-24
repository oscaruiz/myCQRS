package com.oscaruiz.mycqrs.demovanilla.order.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;

import java.util.UUID;

public record ConfirmOrderCommand(UUID commandId, UUID orderId) implements Command {}
