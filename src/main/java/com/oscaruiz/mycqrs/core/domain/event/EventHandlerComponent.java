package com.oscaruiz.mycqrs.core.domain.event;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EventHandlerComponent {
}
