package com.oscaruiz.mycqrs.core.contracts.command;

/**
 * Thrown when an attempt is made to register a {@link CommandHandler} for a
 * command type that already has a handler registered with the {@link CommandBus}.
 *
 * <p>In CQRS, each command type must have exactly one handler. Allowing silent
 * overrides would hide configuration mistakes, so the bus fails fast. This is a
 * wiring/configuration error rather than a recoverable runtime condition, so it
 * is modeled as an unchecked exception. It is part of the public contract of
 * {@link CommandBus} and may be thrown by any implementation.
 */
public class DuplicateCommandHandlerException extends RuntimeException {

    public DuplicateCommandHandlerException(Class<? extends Command> commandType) {
        super("Handler already registered for command type: " + commandType.getName());
    }
}
