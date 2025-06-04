package com.oscaruiz.mycqrs.command;

/**
 * Interface for sending commands and registering their handlers.
 */
public interface CommandBus {

    /**
     * Sends the given command to its registered handler.
     *
     * @param command the command to send
     * @param <CommandType> the type of the command
     * @param <ReturnType> the expected result type
     * @return the result from the command handler
     */
    <CommandType extends Command, ReturnType> ReturnType send(CommandType command);

    /**
     * Registers a command handler for a specific command type.
     *
     * @param commandType the class of the command
     * @param handler the handler to execute for this command
     * @param <CommandType> the command type
     * @param <ReturnType> the result type
     */
    <CommandType extends Command, ReturnType> void registerHandler(
            Class<CommandType> commandType,
            CommandHandler<CommandType, ReturnType> handler
    );
}
