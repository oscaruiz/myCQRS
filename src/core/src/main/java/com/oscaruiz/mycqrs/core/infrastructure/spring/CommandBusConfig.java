package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class CommandBusConfig {

    @Bean
    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.afterPropertiesSet(); // <-- Forzamos inicialización
        return factoryBean;
    }

    @Bean
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    public CommandBus commandBus(Validator validator) {
        var bus = new SimpleCommandBus();
        bus.addInterceptor(new ValidationCommandInterceptor(validator));
        return bus;
    }

    @Bean
    public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(EventBus eventBus) {
        return new EventHandlerBeanPostProcessor(eventBus);
    }
}