package com.oscaruiz.mycqrs.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple in-memory implementation of EventBus.
 * Dispatches events synchronously to all registered handlers.
 */
public class SimpleEventBus implements EventBus {

    private final Map<Class<? extends Event>, List<EventHandler<? extends Event>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void publish(Event event) {
        List<EventHandler<? extends Event>> registeredHandlers = handlers.get(event.getClass());

        if (registeredHandlers != null) {
            for (EventHandler handler : registeredHandlers) {
                //noinspection unchecked
                handler.on(event);
            }
        }
    }

    @Override
    public <EventType extends Event> void registerHandler(
            Class<EventType> eventType,
            EventHandler<EventType> handler
    ) {
        handlers
                .computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>())
                .add(handler);
    }
}
