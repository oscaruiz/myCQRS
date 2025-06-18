package com.oscaruiz.mycqrs.core.domain.command;
/**
 * Generic interface for handling commands.
 *
 * @param <CommandType> the type of command to handle
 * @param <ReturnType> the type of result returned
 */
@FunctionalInterface
public interface CommandHandler<CommandType extends Command, ReturnType> {
    ReturnType handle(CommandType command);
}
