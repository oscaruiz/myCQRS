package com.oscaruiz.mycqrs.core.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Activates the myCQRS framework in a Spring application.
 *
 * <p>Place this annotation on a {@code @Configuration} class (typically the
 * main Spring Boot application class) to register the default {@code CommandBus},
 * {@code QueryBus}, {@code EventBus}, and their supporting infrastructure beans.
 *
 * <p>Every default bean is guarded by {@code @ConditionalOnMissingBean}, so the
 * consumer can replace any component with a custom implementation simply by
 * declaring its own {@code @Bean} of the same type.
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableCqrs
 * public class MyApp { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CqrsDeferredImportSelector.class)
public @interface EnableCqrs {
}
