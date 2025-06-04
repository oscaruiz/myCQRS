package com.oscaruiz.mycqrs.spring;

import com.oscaruiz.mycqrs.command.CommandBus;
import com.oscaruiz.mycqrs.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.event.EventBus;
import com.oscaruiz.mycqrs.event.SimpleEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandBusConfig {

    @Bean
    public CommandBus commandBus() {
        var bus = new SimpleCommandBus();
        bus.addInterceptor(new ValidationCommandInterceptor()); // Validation interceptor
        return bus;
    }

    @Bean
    public EventBus eventBus() {
        return new SimpleEventBus();
    }
}
