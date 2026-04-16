package com.oscaruiz.mycqrs.core.infrastructure.bus.event;
import com.oscaruiz.mycqrs.core.contracts.event.Event;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple in-memory implementation of EventBus.
 * Dispatches events synchronously to all registered handlers.
 */
public class SimpleEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(SimpleEventBus.class);

    private final Map<Class<? extends Event>, CopyOnWriteArrayList<EventHandler<? extends Event>>> handlers = new ConcurrentHashMap<>();

    @Override
    public <T extends Event> void publish(T event) {
        log.debug("POSTING EVENT: {}", event);
        CopyOnWriteArrayList<EventHandler<? extends Event>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler<? extends Event> handler : eventHandlers) {
                @SuppressWarnings("unchecked")
                EventHandler<T> typedHandler = (EventHandler<T>) handler;
                typedHandler.handle(event);
            }
        }
    }

    @Override
    public <T extends Event> void registerHandler(Class<T> type, EventHandler<T> handler) {
        log.debug("REGISTERING HANDLER: {}", handler);
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
}
