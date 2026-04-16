package com.oscaruiz.mycqrs.core.infrastructure.spring;


import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.query.SimpleQueryBus;
/**
 * Factory methods for the query-side infrastructure.
 *
 * <p>No longer a {@code @Configuration} — beans are declared in
 * {@link com.oscaruiz.mycqrs.core.spring.CqrsConfiguration}.
 */
public class QueryBusConfig {

    public QueryBus queryBus() {
        return new SimpleQueryBus();
    }

    public QueryHandlerBeanPostProcessor queryHandlerBeanPostProcessor(QueryBus queryBus) {
        return new QueryHandlerBeanPostProcessor(queryBus);
    }
}
