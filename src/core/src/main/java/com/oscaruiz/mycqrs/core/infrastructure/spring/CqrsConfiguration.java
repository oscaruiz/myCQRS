package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.query.SimpleQueryBus;
import jakarta.validation.Validator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Central configuration for the myCQRS framework.
 *
 * <p>Activated exclusively via {@link EnableCqrs} ({@code @Import}).
 * Declares every framework bean explicitly (no component scanning) and guards
 * each one with {@link ConditionalOnMissingBean} so the consumer can override
 * any default with a custom implementation.
 */
@Configuration(proxyBeanMethods = false)
public class CqrsConfiguration {

    @Bean
    @ConditionalOnMissingBean(Validator.class)
    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    @Bean
    @ConditionalOnMissingBean(CommandBus.class)
    public CommandBus commandBus(Validator validator, PlatformTransactionManager transactionManager) {
        var bus = new SimpleCommandBus();
        bus.addInterceptor(new ValidationCommandInterceptor(validator));
        bus.addInterceptor(new TransactionalCommandInterceptor(transactionManager));
        return bus;
    }

    @Bean
    @ConditionalOnMissingBean(QueryBus.class)
    public QueryBus queryBus() {
        return new SimpleQueryBus();
    }

    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    @ConditionalOnMissingBean(CommandHandlerBeanPostProcessor.class)
    public CommandHandlerBeanPostProcessor commandHandlerBeanPostProcessor(CommandBus commandBus) {
        return new CommandHandlerBeanPostProcessor(commandBus);
    }

    @Bean
    @ConditionalOnMissingBean(QueryHandlerBeanPostProcessor.class)
    public QueryHandlerBeanPostProcessor queryHandlerBeanPostProcessor(QueryBus queryBus) {
        return new QueryHandlerBeanPostProcessor(queryBus);
    }

    @Bean
    @ConditionalOnMissingBean(EventHandlerBeanPostProcessor.class)
    public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(EventBus eventBus) {
        return new EventHandlerBeanPostProcessor(eventBus);
    }
}
