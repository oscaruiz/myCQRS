package com.oscaruiz.mycqrs.core.infrastructure.bus.event;
import com.oscaruiz.mycqrs.core.domain.event.Event;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.domain.event.EventHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple in-memory implementation of EventBus.
 * Dispatches events synchronously to all registered handlers.
 */
public class SimpleEventBus implements EventBus {

    private final Map<Class<? extends Event>, CopyOnWriteArrayList<EventHandler<? extends Event>>> handlers = new ConcurrentHashMap<>();

    @Override
    public <T extends Event> void publish(T event) {
        System.out.print("POSTING EVENT: "+event);
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
        System.out.print("REGISTERING HANDLER: "+handler);
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
}