package com.oscaruiz.mycqrs.core.domain.query;

/**
 * Thrown when a query is dispatched through the {@link QueryBus} but no
 * {@link QueryHandler} has been registered for its type.
 *
 * <p>This represents a wiring/configuration error rather than a recoverable
 * runtime condition, so it is modeled as an unchecked exception. It is part of
 * the public contract of {@link QueryBus} and may be thrown by any
 * implementation.
 */
public class QueryHandlerNotFoundException extends RuntimeException {

    public QueryHandlerNotFoundException(Class<? extends Query<?>> queryType) {
        super("No handler registered for query type: " + queryType.getName());
    }
}
