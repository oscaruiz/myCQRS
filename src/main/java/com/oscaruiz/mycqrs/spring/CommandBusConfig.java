package com.oscaruiz.mycqrs.spring;

import com.oscaruiz.mycqrs.command.CommandBus;
import com.oscaruiz.mycqrs.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.event.EventBus;
import com.oscaruiz.mycqrs.event.SimpleEventBus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandBusConfig {

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    public CommandBus commandBus(Validator validator, EventBus eventBus) {
        // TODO - REVISE
        var bus = new SimpleCommandBus();
        bus.setEventBus(eventBus);
        bus.addInterceptor(new ValidationCommandInterceptor(validator));
        return bus;
    }

    @Bean
    public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(EventBus eventBus) {
        return new EventHandlerBeanPostProcessor(eventBus);
    }
}
