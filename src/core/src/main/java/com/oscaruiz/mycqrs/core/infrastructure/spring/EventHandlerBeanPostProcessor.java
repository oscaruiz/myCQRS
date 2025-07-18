package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.domain.event.Event;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
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
        System.out.println("🔍 Post-procesando bean: " + beanName + " (" + bean.getClass().getSimpleName() + ")");
        if (!(bean instanceof EventHandler<?> handler)) {
            return bean;
        }

        ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(EventHandler.class);
        Class<?> eventType = resolvableType.getGeneric(0).resolve();

        if (eventType != null && Event.class.isAssignableFrom(eventType)) {
            System.out.println("📣 Registrando handler para: " + eventType.getSimpleName());
            eventBus.registerHandler((Class<? extends Event>) eventType, event -> {
                ((EventHandler<Event>) handler).handle(event);
            });
            System.out.println("📣 Registered event handler for: " + eventType.getSimpleName());
        }

        return bean;
    }
}
