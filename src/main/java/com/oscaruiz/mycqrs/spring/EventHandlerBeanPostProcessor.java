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
        if (!(bean instanceof EventHandler<?> handler)) {
            return bean;
        }

        // Resuelve el tipo concreto del evento
        ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(EventHandler.class);
        Class<?> eventType = resolvableType.getGeneric(0).resolve();

        if (eventType != null && Event.class.isAssignableFrom(eventType)) {
            eventBus.registerHandler((Class<? extends Event>) eventType, event -> {
                ((EventHandler<Event>) handler).on(event);
            });
            System.out.println("📣 Registered event handler for: " + eventType.getSimpleName());
        }

        return bean;
    }
}
