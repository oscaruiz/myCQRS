package com.oscaruiz.mycqrs.query;

public interface QueryBus {
    <R, Q extends Query<R>> R handle(Q query);

    <Q extends Query<R>, R> void registerHandler(Class<Q> queryType, QueryHandler<Q, R> handler);

}
