package com.oscaruiz.mycqrs.demovanilla.infrastructure.bus;

import com.oscaruiz.mycqrs.core.contracts.query.Query;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Decorator rather than modifying core because core does not yet have a
// QueryInterceptor pipeline (pending ADR 0014).
public class LoggingQueryBus implements QueryBus {

    private static final Logger log = LoggerFactory.getLogger(LoggingQueryBus.class);

    private final QueryBus delegate;

    public LoggingQueryBus(QueryBus delegate) {
        this.delegate = delegate;
    }

    @Override
    public <R, Q extends Query<R>> R handle(Q query) {
        String name = query.getClass().getSimpleName();
        long start = System.currentTimeMillis();
        log.debug("QUERY → {}", name);
        try {
            R result = delegate.handle(query);
            log.debug("QUERY ← {} ({}ms) result={}", name, System.currentTimeMillis() - start, result);
            return result;
        } catch (RuntimeException e) {
            log.debug("QUERY ✗ {} ({}ms) — {}", name, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    @Override
    public <Q extends Query<R>, R> void registerHandler(Class<Q> queryType, QueryHandler<Q, R> handler) {
        delegate.registerHandler(queryType, handler);
    }
}
