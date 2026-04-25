package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.Event;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.contracts.query.Query;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import io.micronaut.context.BeanContext;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Auto-registers every {@link CommandHandler}, {@link QueryHandler} and
 * {@link EventHandler} bean with the matching bus on application startup.
 *
 * <p>The analogous Spring adapter does this via {@code BeanPostProcessor}s (one per
 * handler type), which fire eagerly as each bean is initialised. Micronaut does not
 * expose a comparable per-bean hook; the idiomatic replacement is a
 * {@link StartupEvent} listener that walks the resolved context once all singleton
 * beans are ready.
 *
 * <p>Handler-to-type resolution uses Micronaut's compile-time type metadata
 * ({@link BeanDefinition#getTypeArguments(Class)}) so this registrar does not rely
 * on runtime reflection of generics.
 */
@Singleton
public class MicronautHandlerRegistrar implements ApplicationEventListener<StartupEvent> {

    private static final Logger log = LoggerFactory.getLogger(MicronautHandlerRegistrar.class);

    private final BeanContext beanContext;
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final EventBus eventBus;

    public MicronautHandlerRegistrar(BeanContext beanContext,
                                     CommandBus commandBus,
                                     QueryBus queryBus,
                                     EventBus eventBus) {
        this.beanContext = beanContext;
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.eventBus = eventBus;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        registerCommandHandlers();
        registerQueryHandlers();
        registerEventHandlers();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerCommandHandlers() {
        Collection<BeanDefinition<CommandHandler>> definitions =
                beanContext.getBeanDefinitions(CommandHandler.class);
        for (BeanDefinition<CommandHandler> definition : definitions) {
            List<Argument<?>> typeArguments = definition.getTypeArguments(CommandHandler.class);
            if (typeArguments.isEmpty()) {
                continue;
            }
            Class<?> commandType = typeArguments.get(0).getType();
            if (!Command.class.isAssignableFrom(commandType)) {
                continue;
            }
            CommandHandler handler = beanContext.getBean(definition);
            commandBus.registerHandler((Class) commandType, handler);
            log.info("Registered command handler: {} -> {}",
                    commandType.getSimpleName(),
                    handler.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerQueryHandlers() {
        Collection<BeanDefinition<QueryHandler>> definitions =
                beanContext.getBeanDefinitions(QueryHandler.class);
        for (BeanDefinition<QueryHandler> definition : definitions) {
            List<Argument<?>> typeArguments = definition.getTypeArguments(QueryHandler.class);
            if (typeArguments.isEmpty()) {
                continue;
            }
            Class<?> queryType = typeArguments.get(0).getType();
            if (!Query.class.isAssignableFrom(queryType)) {
                continue;
            }
            QueryHandler handler = beanContext.getBean(definition);
            queryBus.registerHandler((Class) queryType, handler);
            log.info("Registered query handler: {} -> {}",
                    queryType.getSimpleName(),
                    handler.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerEventHandlers() {
        Collection<BeanDefinition<EventHandler>> definitions =
                beanContext.getBeanDefinitions(EventHandler.class);
        for (BeanDefinition<EventHandler> definition : definitions) {
            List<Argument<?>> typeArguments = definition.getTypeArguments(EventHandler.class);
            if (typeArguments.isEmpty()) {
                continue;
            }
            Class<?> eventType = typeArguments.get(0).getType();
            if (!Event.class.isAssignableFrom(eventType)) {
                continue;
            }
            EventHandler handler = beanContext.getBean(definition);
            eventBus.registerHandler((Class) eventType, handler);
            log.info("Registered event handler: {} -> {}",
                    eventType.getSimpleName(),
                    handler.getClass().getSimpleName());
        }
    }
}
