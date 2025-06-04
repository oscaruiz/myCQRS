package com.oscaruiz.mycqrs.event;

/**
 * Generic interface for handling events.
 *
 * @param <EventType> the type of event
 */
@FunctionalInterface
public interface EventHandler<EventType extends Event> {
    void on(EventType event);
}
