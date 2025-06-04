package com.oscaruiz.mycqrs.event;

/**
 * Interface for handling events.
 *
 * @param <EventType> the type of event
 */
@FunctionalInterface
public interface EventHandler<EventType extends Event> {
    void on(EventType event);
}
