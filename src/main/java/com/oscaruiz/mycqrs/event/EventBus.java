package com.oscaruiz.mycqrs.event;

/**
 * Interface for publishing and subscribing to domain events.
 */
public interface EventBus {

    /**
     * Publishes an event to all registered handlers.
     *
     * @param event the event to publish
     */
    <T extends Event> void publish(T event);

    /**
     * Registers a handler for a specific event type.
     *
     * @param eventType the type of event
     * @param handler the handler for that event type
     * @param <EventType> the event class
     */
    <EventType extends Event> void registerHandler(Class<EventType> eventType, EventHandler<EventType> handler);
}
