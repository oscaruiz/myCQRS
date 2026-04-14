package com.oscaruiz.mycqrs.core.contracts.query;

/**
 * Thrown when an attempt is made to register a {@link QueryHandler} for a
 * query type that already has a handler registered with the {@link QueryBus}.
 *
 * <p>In CQRS, each query type must have exactly one handler. Allowing silent
 * overrides would hide configuration mistakes, so the bus fails fast. This is a
 * wiring/configuration error rather than a recoverable runtime condition, so it
 * is modeled as an unchecked exception. It is part of the public contract of
 * {@link QueryBus} and may be thrown by any implementation.
 */
public class DuplicateQueryHandlerException extends RuntimeException {

    public DuplicateQueryHandlerException(Class<? extends Query<?>> queryType) {
        super("Handler already registered for query type: " + queryType.getName());
    }
}
