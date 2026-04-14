package com.oscaruiz.mycqrs.core.contracts.command;

/**
 * Thrown when a command is dispatched through the {@link CommandBus} but no
 * {@link CommandHandler} has been registered for its type.
 *
 * <p>This represents a wiring/configuration error rather than a recoverable
 * runtime condition, so it is modeled as an unchecked exception. It is part of
 * the public contract of {@link CommandBus} and may be thrown by any
 * implementation.
 */
public class CommandHandlerNotFoundException extends RuntimeException {

    public CommandHandlerNotFoundException(Class<? extends Command> commandType) {
        super("No handler registered for command type: " + commandType.getName());
    }
}
