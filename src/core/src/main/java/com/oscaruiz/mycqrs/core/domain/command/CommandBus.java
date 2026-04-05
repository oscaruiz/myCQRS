package com.oscaruiz.mycqrs.core.domain.command;
/**
 * Interface for sending commands and registering their handlers.
 */
public interface CommandBus {

    /**
     * Sends the given command to its registered handler.
     *
     * @param command the command to send
     * @param <CommandType> the type of the command
     * @return the result from the command handler
     */
    <CommandType extends Command> void send(CommandType command);

    /**
     * Registers a command handler for a specific command type.
     *
     * @param commandType the class of the command
     * @param handler the handler to execute for this command
     * @param <CommandType> the command type
     */
    <CommandType extends Command> void registerHandler(
            Class<CommandType> commandType,
            CommandHandler<CommandType> handler
    );
}
