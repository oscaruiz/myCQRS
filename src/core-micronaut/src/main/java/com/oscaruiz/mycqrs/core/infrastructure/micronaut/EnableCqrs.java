package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Source-level marker for symmetry with the Spring adapter's {@code @EnableCqrs}.
 *
 * <p>Unlike the Spring adapter, this annotation has <strong>no runtime effect</strong>.
 * Micronaut discovers the CQRS beans via compile-time annotation processing on
 * {@link CqrsFactory} and {@link MicronautHandlerRegistrar}, so adding
 * {@code mycqrs-micronaut} to the classpath is sufficient to activate the framework.
 *
 * <p>Present to keep consumer code shape consistent across adapters: a reader
 * comparing a Spring consumer to a Micronaut consumer sees the same activation
 * annotation on the application class. Remove at any time — nothing in this module
 * reads it.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableCqrs {
}
