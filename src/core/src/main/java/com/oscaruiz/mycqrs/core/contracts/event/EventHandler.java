package com.oscaruiz.mycqrs.core.contracts.event;
/**
 * Interface for handling events.
 *
 * @param <EventType> the type of event
 */
@FunctionalInterface
public interface EventHandler<EventType extends Event> {
    void handle(EventType event);
}
