package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.idempotency.IdempotencyCommandInterceptor;
import com.oscaruiz.mycqrs.core.idempotency.ProcessedCommandsStore;
import com.oscaruiz.mycqrs.core.infrastructure.bus.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.query.SimpleQueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.observability.CorrelationIdCommandInterceptor;
import com.oscaruiz.mycqrs.core.infrastructure.spring.idempotency.JdbcProcessedCommandsStore;
import jakarta.validation.Validator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Central configuration for the myCQRS framework.
 *
 * <p>Activated exclusively via {@link EnableCqrs} ({@code @Import}).
 * Declares every framework bean explicitly (no component scanning) and guards
 * each one with {@link ConditionalOnMissingBean} so the consumer can override
 * any default with a custom implementation.
 */
@Configuration(proxyBeanMethods = false)
public class CqrsConfiguration {

    @Bean
    @ConditionalOnMissingBean(Validator.class)
    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    @Bean
    @ConditionalOnMissingBean(ProcessedCommandsStore.class)
    public ProcessedCommandsStore processedCommandsStore(NamedParameterJdbcTemplate jdbc) {
        return new JdbcProcessedCommandsStore(jdbc);
    }

    /**
     * Interceptors are applied by {@link SimpleCommandBus} such that the first registered
     * interceptor is the outermost wrapper, so this list produces the execution order
     * CorrelationId → Validation → Transactional → Idempotency → Handler. CorrelationId
     * stays outermost so validation failures and transactional rollbacks are logged with
     * the trace key. Idempotency stays innermost so the {@code processed_commands} insert
     * commits or rolls back together with the handler's side effects.
     */
    @Bean
    @ConditionalOnMissingBean(CommandBus.class)
    public CommandBus commandBus(Validator validator,
                                 PlatformTransactionManager transactionManager,
                                 ProcessedCommandsStore processedCommandsStore) {
        var bus = new SimpleCommandBus();
        bus.addInterceptor(new CorrelationIdCommandInterceptor());
        bus.addInterceptor(new ValidationCommandInterceptor(validator));
        bus.addInterceptor(new TransactionalCommandInterceptor(transactionManager));
        bus.addInterceptor(new IdempotencyCommandInterceptor(processedCommandsStore));
        return bus;
    }

    @Bean
    @ConditionalOnMissingBean(QueryBus.class)
    public QueryBus queryBus() {
        return new SimpleQueryBus();
    }

    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    @ConditionalOnMissingBean(CommandHandlerBeanPostProcessor.class)
    public CommandHandlerBeanPostProcessor commandHandlerBeanPostProcessor(CommandBus commandBus) {
        return new CommandHandlerBeanPostProcessor(commandBus);
    }

    @Bean
    @ConditionalOnMissingBean(QueryHandlerBeanPostProcessor.class)
    public QueryHandlerBeanPostProcessor queryHandlerBeanPostProcessor(QueryBus queryBus) {
        return new QueryHandlerBeanPostProcessor(queryBus);
    }

    @Bean
    @ConditionalOnMissingBean(EventHandlerBeanPostProcessor.class)
    public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(EventBus eventBus) {
        return new EventHandlerBeanPostProcessor(eventBus);
    }
}
