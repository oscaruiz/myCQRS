package com.oscaruiz.mycqrs.core.infrastructure.spring;


import com.oscaruiz.mycqrs.core.domain.query.QueryBus;
import com.oscaruiz.mycqrs.core.domain.query.SimpleQueryBus;
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
