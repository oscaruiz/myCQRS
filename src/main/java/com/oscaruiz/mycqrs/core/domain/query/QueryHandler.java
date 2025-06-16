package com.oscaruiz.mycqrs.core.domain.query;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
