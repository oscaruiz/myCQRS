package com.oscaruiz.mycqrs.core.infrastructure.bus.command;

import com.oscaruiz.mycqrs.core.domain.command.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple in-memory implementation of CommandBus with interceptor support.
 */
public class SimpleCommandBus implements CommandBus {

    private final Map<Class<? extends Command>, CommandHandler<?>> handlers = new ConcurrentHashMap<>();
    private final List<CommandInterceptor> interceptors = new CopyOnWriteArrayList<>();

    @Override
    public <CommandType extends Command> void send(CommandType command) {
        @SuppressWarnings("unchecked")
        CommandHandler<CommandType> handler = (CommandHandler<CommandType>) Optional
            .ofNullable(handlers.get(command.getClass()))
            .orElseThrow(() -> new CommandHandlerNotFoundException(command.getClass()));

        // Define the core handler invocation
        CommandInterceptor.CommandHandlerInvoker invoker = cmd -> handler.handle(command);

        // Wrap with interceptors
        applyInterceptors(command, invoker);
    }

    private void applyInterceptors(Command command, CommandInterceptor.CommandHandlerInvoker target) {
        CommandInterceptor.CommandHandlerInvoker chain = target;

        for (int i = interceptors.size() - 1; i >= 0; i--) {
            CommandInterceptor interceptor = interceptors.get(i);
            CommandInterceptor.CommandHandlerInvoker next = chain;
            chain = cmd -> interceptor.intercept(cmd, next);
        }

        chain.invoke(command);
    }

    @Override
    public <CommandType extends Command> void registerHandler(
        Class<CommandType> commandType,
        CommandHandler<CommandType> handler
    ) {
        if (handlers.putIfAbsent(commandType, handler) != null) {
            throw new DuplicateCommandHandlerException(commandType);
        }
    }

    /**
     * Registers a command interceptor to be applied around handler execution.
     *
     * @param interceptor the interceptor to add
     */
    public void addInterceptor(CommandInterceptor interceptor) {
        interceptors.add(interceptor);
    }

}
