package com.oscaruiz.mycqrs.core.idempotency;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guarantees exactly-once side effects for command processing.
 *
 * <p>Must be registered as the INNERMOST interceptor of the chain so that it runs
 * inside the transaction opened by {@code TransactionalCommandInterceptor}. The
 * single-write atomicity of {@link ProcessedCommandsStore#markProcessedIfAbsent}
 * combined with the enclosing transaction guarantees that the handler's side
 * effects and the processed-commands row commit or roll back together.
 */
public class IdempotencyCommandInterceptor implements CommandInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCommandInterceptor.class);

    private final ProcessedCommandsStore store;

    public IdempotencyCommandInterceptor(ProcessedCommandsStore store) {
        this.store = store;
    }

    @Override
    public void intercept(Command command, CommandHandlerInvoker next) {
        String commandType = command.getClass().getSimpleName();
        boolean firstTime = store.markProcessedIfAbsent(command.commandId(), commandType);
        if (!firstTime) {
            log.info("Command {} of type {} already processed; skipping", command.commandId(), commandType);
            return;
        }
        next.invoke(command);
    }
}
