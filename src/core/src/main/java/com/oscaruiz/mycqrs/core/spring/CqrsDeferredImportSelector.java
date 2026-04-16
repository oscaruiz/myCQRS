package com.oscaruiz.mycqrs.core.spring;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Defers the import of {@link CqrsConfiguration} until after all regular
 * {@code @Configuration} classes have been parsed.
 *
 * <p>This guarantees that {@code @ConditionalOnMissingBean} checks in
 * {@code CqrsConfiguration} see every bean defined by the consumer — including
 * those declared as {@code @Bean} methods on the same class that carries
 * {@code @EnableCqrs}.
 */
class CqrsDeferredImportSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] { CqrsConfiguration.class.getName() };
    }
}
