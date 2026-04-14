package com.oscaruiz.mycqrs.core.infrastructure.bus.query;

import com.oscaruiz.mycqrs.core.contracts.query.DuplicateQueryHandlerException;
import com.oscaruiz.mycqrs.core.contracts.query.Query;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import com.oscaruiz.mycqrs.core.contracts.query.QueryHandlerNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleQueryBus implements QueryBus {

    private final Map<Class<? extends Query<?>>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    public <Q extends Query<R>, R> void registerHandler(Class<Q> queryType, QueryHandler<Q, R> handler) {
        if (handlers.putIfAbsent(queryType, handler) != null) {
            throw new DuplicateQueryHandlerException(queryType);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, Q extends Query<R>> R handle(Q query) {
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(query.getClass());
        if (handler == null) {
            Class<? extends Query<?>> queryType = (Class<? extends Query<?>>) query.getClass();
            throw new QueryHandlerNotFoundException(queryType);
        }
        return handler.handle(query);
    }
}
