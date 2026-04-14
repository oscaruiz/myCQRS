package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

/**
 * BeanPostProcessor that automatically registers CommandHandler implementations with the CommandBus.
 */
@Component
public class CommandHandlerBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(CommandHandlerBeanPostProcessor.class);

    private final CommandBus commandBus;

    public CommandHandlerBeanPostProcessor(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof CommandHandler<?>)) {
            return bean;
        }

        ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(CommandHandler.class);

        Class<?> genericCommandType = resolvableType.getGeneric(0).resolve();

        if (genericCommandType == null || !Command.class.isAssignableFrom(genericCommandType)) {
            return bean;
        }

        registerHandler(genericCommandType, (CommandHandler<?>) bean);

        return bean;
    }

    @SuppressWarnings("unchecked")
    private <C extends Command, R> void registerHandler(
            Class<?> commandTypeRaw,
            CommandHandler<?> handlerRaw
    ) {
        Class<C> commandType = (Class<C>) commandTypeRaw;
        CommandHandler<C> handler = (CommandHandler<C>) handlerRaw;

        commandBus.registerHandler(commandType, handler);
        log.info("Registered handler for command: {}", commandType.getSimpleName());
    }

}
