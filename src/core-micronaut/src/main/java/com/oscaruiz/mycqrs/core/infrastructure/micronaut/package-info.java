/**
 * Micronaut adapter for the myCQRS framework.
 *
 * <p>Consumers add this module to their classpath and Micronaut's compile-time bean
 * discovery takes care of wiring. Unlike the Spring adapter, there is no explicit
 * activation annotation required for the factory to register beans — the
 * {@link com.oscaruiz.mycqrs.core.infrastructure.micronaut.EnableCqrs} marker is
 * provided only for source-level symmetry with
 * {@code com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs}.
 *
 * <p>Interceptor chain wired by {@link CqrsFactory}:
 * {@code CorrelationId → Validation → Transactional}. Idempotency is intentionally
 * omitted from this module — see ADR 0016. Consumers who need
 * {@link com.oscaruiz.mycqrs.core.infrastructure.idempotency.IdempotencyCommandInterceptor} must
 * provide their own {@link com.oscaruiz.mycqrs.core.infrastructure.idempotency.ProcessedCommandsStore}
 * bean and register the interceptor manually.
 */
package com.oscaruiz.mycqrs.core.infrastructure.micronaut;
