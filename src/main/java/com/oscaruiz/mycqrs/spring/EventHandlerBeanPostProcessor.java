package com.oscaruiz.mycqrs.spring;

import com.oscaruiz.mycqrs.event.Event;
import com.oscaruiz.mycqrs.event.EventBus;
import com.oscaruiz.mycqrs.event.EventHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

@Component
public class EventHandlerBeanPostProcessor implements BeanPostProcessor {

    private final EventBus eventBus;

    public EventHandlerBeanPostProcessor(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof EventHandler<?>)) {
            return bean;
        }

        ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(EventHandler.class);
        Class<?> rawEventType = resolvableType.getGeneric(0).resolve();

        if (rawEventType == null || !Event.class.isAssignableFrom(rawEventType)) {
            return bean;
        }

        registerHandler(rawEventType, (EventHandler<?>) bean);

        return bean;
    }

    @SuppressWarnings("unchecked")
    private <E extends Event> void registerHandler(
            Class<?> rawType,
            EventHandler<?> rawHandler
    ) {
        Class<E> eventType = (Class<E>) rawType;
        EventHandler<E> handler = (EventHandler<E>) rawHandler;

        eventBus.registerHandler(eventType, handler);
        System.out.println("📣 Registered event handler for: " + eventType.getSimpleName());
    }
}
