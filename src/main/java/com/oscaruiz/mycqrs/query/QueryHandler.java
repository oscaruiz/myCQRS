package com.oscaruiz.mycqrs.query;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
