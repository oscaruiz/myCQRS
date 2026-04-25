package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor;
import io.micronaut.transaction.SynchronousTransactionManager;

/**
 * Wraps command execution in a Micronaut-managed transaction.
 *
 * <p>Delegates rollback/commit semantics to
 * {@link SynchronousTransactionManager#executeWrite(io.micronaut.transaction.TransactionCallback)
 * executeWrite}, which rolls back on any thrown exception and commits otherwise —
 * the same behaviour the Spring adapter achieves manually.
 */
public class TransactionalCommandInterceptor implements CommandInterceptor {

    private final SynchronousTransactionManager<?> transactionManager;

    public TransactionalCommandInterceptor(SynchronousTransactionManager<?> transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void intercept(Command command, CommandHandlerInvoker next) {
        transactionManager.executeWrite(status -> {
            next.invoke(command);
            return null;
        });
    }
}
