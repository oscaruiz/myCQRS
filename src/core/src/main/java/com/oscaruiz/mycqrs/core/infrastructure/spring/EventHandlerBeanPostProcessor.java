package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.domain.event.Event;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

@Component
public class EventHandlerBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventHandlerBeanPostProcessor.class);

    private final EventBus eventBus;

    public EventHandlerBeanPostProcessor(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        log.debug("Processing bean: {} ({})", beanName, bean.getClass().getSimpleName());
        if (!(bean instanceof EventHandler<?> handler)) {
            return bean;
        }

        ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(EventHandler.class);
        Class<?> eventType = resolvableType.getGeneric(0).resolve();

        if (eventType != null && Event.class.isAssignableFrom(eventType)) {
            log.info("Registering event handler for: {}", eventType.getSimpleName());
            eventBus.registerHandler((Class<? extends Event>) eventType, event -> {
                ((EventHandler<Event>) handler).handle(event);
            });
            log.info("Registered event handler for: {}", eventType.getSimpleName());
        }

        return bean;
    }
}
