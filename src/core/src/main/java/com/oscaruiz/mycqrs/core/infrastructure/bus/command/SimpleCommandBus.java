package com.oscaruiz.mycqrs.core.infrastructure.bus.command;
import com.oscaruiz.mycqrs.core.domain.command.Command;
import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.domain.command.CommandHandler;
import com.oscaruiz.mycqrs.core.domain.command.CommandInterceptor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple in-memory implementation of CommandBus with interceptor support.
 */
public class SimpleCommandBus implements CommandBus {

    private final Map<Class<? extends Command>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final List<CommandInterceptor> interceptors = new CopyOnWriteArrayList<>();

    @Override
    public <CommandType extends Command, ReturnType> ReturnType send(CommandType command) {
        @SuppressWarnings("unchecked")
        CommandHandler<CommandType, ReturnType> handler =
                (CommandHandler<CommandType, ReturnType>) handlers.get(command.getClass());

        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for command type: " + command.getClass().getName());
        }

        // Define the core handler invocation
        CommandInterceptor.CommandHandlerInvoker invoker = cmd -> handler.handle(command);

        // Wrap with interceptors
        Object result = applyInterceptors(command, invoker);
        return (ReturnType) result;
    }

    private Object applyInterceptors(Command command, CommandInterceptor.CommandHandlerInvoker target) {
        CommandInterceptor.CommandHandlerInvoker chain = target;

        for (int i = interceptors.size() - 1; i >= 0; i--) {
            CommandInterceptor interceptor = interceptors.get(i);
            CommandInterceptor.CommandHandlerInvoker next = chain;
            chain = cmd -> interceptor.intercept(cmd, next);
        }

        return chain.invoke(command);
    }

    @Override
    public <CommandType extends Command, ReturnType> void registerHandler(
            Class<CommandType> commandType,
            CommandHandler<CommandType, ReturnType> handler
    ) {
        if (handlers.containsKey(commandType)) {
            throw new IllegalStateException("Handler already registered for command type: " + commandType.getName());
        }

        handlers.put(commandType, handler);
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
