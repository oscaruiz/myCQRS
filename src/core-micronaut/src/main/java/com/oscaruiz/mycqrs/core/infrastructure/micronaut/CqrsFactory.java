package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.query.SimpleQueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.observability.CorrelationIdCommandInterceptor;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.transaction.SynchronousTransactionManager;
import jakarta.inject.Singleton;
import jakarta.validation.Validator;

/**
 * Central factory for the myCQRS framework beans in a Micronaut application.
 *
 * <p>Each bean is marked {@code @Singleton} and registered only if the consumer has
 * not already provided one — Micronaut's default resolution picks the consumer's
 * bean over the framework default when duplicate types exist.
 *
 * <p>Interceptor order (produced by {@link SimpleCommandBus#addInterceptor}):
 * {@code CorrelationId → Validation → Transactional → Handler}. The transactional
 * interceptor is omitted when no {@link SynchronousTransactionManager} bean is
 * available, so the module can be activated in tests or minimal apps without a
 * DataSource.
 */
@Factory
public class CqrsFactory {

    @Singleton
    public CommandBus commandBus(Validator validator,
                                 @Nullable SynchronousTransactionManager<?> transactionManager) {
        SimpleCommandBus bus = new SimpleCommandBus();
        bus.addInterceptor(new CorrelationIdCommandInterceptor());
        bus.addInterceptor(new ValidationCommandInterceptor(validator));
        if (transactionManager != null) {
            bus.addInterceptor(new TransactionalCommandInterceptor(transactionManager));
        }
        return bus;
    }

    @Singleton
    public QueryBus queryBus() {
        return new SimpleQueryBus();
    }

    @Singleton
    public EventBus eventBus() {
        return new SimpleEventBus();
    }
}
