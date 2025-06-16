package com.oscaruiz.mycqrs.core.domain.event;
/**
 * Interface for handling events.
 *
 * @param <EventType> the type of event
 */
@FunctionalInterface
public interface EventHandler<EventType extends Event> {
    void handle(EventType event);
}
