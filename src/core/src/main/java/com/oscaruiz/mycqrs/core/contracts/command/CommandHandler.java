package com.oscaruiz.mycqrs.core.contracts.command;
/**
 * Generic interface for handling commands.
 *
 * @param <CommandType> the type of command to handle
 */
@FunctionalInterface
public interface CommandHandler<CommandType extends Command> {
    void handle(CommandType command);
}
