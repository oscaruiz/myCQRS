package com.oscaruiz.mycqrs.spring;

import com.oscaruiz.mycqrs.command.Command;
import com.oscaruiz.mycqrs.command.CommandBus;
import com.oscaruiz.mycqrs.command.CommandHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

/**
 * BeanPostProcessor that automatically registers CommandHandler implementations with the CommandBus.
 */
@Component
public class CommandHandlerBeanPostProcessor implements BeanPostProcessor {

    private final CommandBus commandBus;

    public CommandHandlerBeanPostProcessor(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof CommandHandler<?, ?>)) {
            return bean;
        }

        ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(CommandHandler.class);

        // TODO REVIEW
        Class<?> genericCommandType = resolvableType.getGeneric(0).resolve();
        Class<?> genericReturnType = resolvableType.getGeneric(1).resolve();

        if (genericCommandType == null || !Command.class.isAssignableFrom(genericCommandType)) {
            return bean;
        }

        registerHandler(genericCommandType, (CommandHandler<?, ?>) bean);

        return bean;
    }

    @SuppressWarnings("unchecked")
    private <C extends Command, R> void registerHandler(
            Class<?> commandTypeRaw,
            CommandHandler<?, ?> handlerRaw
    ) {
        Class<C> commandType = (Class<C>) commandTypeRaw;
        CommandHandler<C, R> handler = (CommandHandler<C, R>) handlerRaw;

        commandBus.registerHandler(commandType, handler);
        System.out.println("✅ Registered handler for command: " + commandType.getSimpleName());
    }

}
