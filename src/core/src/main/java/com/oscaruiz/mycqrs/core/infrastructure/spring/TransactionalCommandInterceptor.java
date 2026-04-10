package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.domain.command.Command;
import com.oscaruiz.mycqrs.core.domain.command.CommandInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Wraps command execution in a Spring-managed transaction.
 *
 * <p>Intended composition order: validation → transaction → handler. Validation is
 * a cheap, in-memory check and must run <em>before</em> a transaction is opened so
 * that invalid commands never touch the database.
 *
 * <p>Rollback semantics mirror Spring's defaults: only {@link RuntimeException} and
 * {@link Error} trigger a rollback. Checked exceptions are not caught here (the
 * current {@code CommandHandler} contract does not declare any anyway).
 *
 * <p>{@link PlatformTransactionManager#commit} runs <em>outside</em> the {@code try}
 * block on purpose: if the commit itself fails, the transaction is already closed
 * and calling {@code rollback} on it would be invalid.
 *
 * <p>TODO move this class to {@code core/spring/} once the core module is
 *  split into {@code api} / {@code infrastructure} / {@code spring} sub-packages.
 */
public class TransactionalCommandInterceptor implements CommandInterceptor {

    private final PlatformTransactionManager transactionManager;

    public TransactionalCommandInterceptor(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void intercept(Command command, CommandHandlerInvoker next) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRED);
        definition.setName("command-" + command.getClass().getSimpleName());

        TransactionStatus status = transactionManager.getTransaction(definition);
        try {
            next.invoke(command);
        } catch (RuntimeException | Error ex) {
            transactionManager.rollback(status);
            throw ex;
        }
        transactionManager.commit(status);
    }
}
