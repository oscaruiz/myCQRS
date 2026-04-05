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
     */
    void intercept(Command command, CommandHandlerInvoker next);

    interface CommandHandlerInvoker {
        void invoke(Command command);
    }
}
