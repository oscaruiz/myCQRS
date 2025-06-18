package com.oscaruiz.mycqrs.core.domain.query;

import java.util.HashMap;
import java.util.Map;

public class SimpleQueryBus implements QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new HashMap<>();

    public <Q extends Query<R>, R> void registerHandler(Class<Q> queryType, QueryHandler<Q, R> handler) {
        handlers.put(queryType, handler);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, Q extends Query<R>> R handle(Q query) {
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalStateException("No handler registered for query: " + query.getClass().getName());
        }
        return handler.handle(query);
    }


}
