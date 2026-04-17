/**
 * Spring adapter for the myCQRS framework.
 *
 * <p>Consumers activate the framework via {@link EnableCqrs}.
 * The rest of this package is internal plumbing (BeanPostProcessors,
 * interceptors, configuration) wired by {@link CqrsConfiguration}
 * and not meant to be imported directly.
 *
 * <p>This package is optional. The core contracts
 * ({@code core.contracts}) and DDD building blocks ({@code core.ddd})
 * do not depend on Spring. A future adapter for another framework
 * (e.g. Micronaut) would live as a sibling package under
 * {@code core.infrastructure}.
 */
package com.oscaruiz.mycqrs.core.infrastructure.spring;
