package com.oscaruiz.mycqrs.core.contracts.query;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
