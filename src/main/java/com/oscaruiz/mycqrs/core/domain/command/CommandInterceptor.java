package com.oscaruiz.mycqrs.core.domain.command;
/**
 * Allows pre-processing or wrapping command execution.
 */
@FunctionalInterface
public interface CommandInterceptor {
    /**
     * Called before or around command execution.
     *
     * @param command the command being sent
     * @param next function to invoke the actual handler
     * @return the result of the command handler
     */
    Object intercept(Command command, CommandHandlerInvoker next);

    interface CommandHandlerInvoker {
        Object invoke(Command command);
    }
}
