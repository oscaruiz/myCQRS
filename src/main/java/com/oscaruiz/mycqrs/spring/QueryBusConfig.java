package com.oscaruiz.mycqrs.spring;

import com.oscaruiz.mycqrs.query.QueryBus;
import com.oscaruiz.mycqrs.query.SimpleQueryBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryBusConfig {

    @Bean
    public QueryBus queryBus() {
        return new SimpleQueryBus();
    }

    @Bean
    public QueryHandlerBeanPostProcessor queryHandlerBeanPostProcessor(QueryBus queryBus) {
        return new QueryHandlerBeanPostProcessor(queryBus);
    }
}
