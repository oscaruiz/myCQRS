package com.oscaruiz.mycqrs.core.infrastructure.spring;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface CommandHandlerComponent {
}
