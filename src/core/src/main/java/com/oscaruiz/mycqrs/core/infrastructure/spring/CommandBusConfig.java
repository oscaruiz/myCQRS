package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.command.SimpleCommandBus;
import jakarta.validation.Validator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Factory methods for the command-side infrastructure.
 *
 * <p>No longer a {@code @Configuration} — beans are declared in
 * {@link com.oscaruiz.mycqrs.core.spring.CqrsConfiguration}.
 */
public class CommandBusConfig {

    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

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
}