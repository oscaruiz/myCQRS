package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class CommandBusConfig {

    @Bean
    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    @Bean
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    public CommandBus commandBus(Validator validator, PlatformTransactionManager transactionManager) {
        var bus = new SimpleCommandBus();
        // Registration order == execution order: SimpleCommandBus.applyInterceptors
        // wraps the chain from last to first, so the first registered interceptor
        // ends up as the outermost wrapper. We want validation (cheap) before the
        // transaction (expensive resource) so it is registered first.
        bus.addInterceptor(new ValidationCommandInterceptor(validator));
        bus.addInterceptor(new TransactionalCommandInterceptor(transactionManager));
        return bus;
    }

    @Bean
    public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(EventBus eventBus) {
        return new EventHandlerBeanPostProcessor(eventBus);
    }
}