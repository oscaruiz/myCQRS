package com.oscaruiz.mycqrs.core.infrastructure.observability;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Populates the SLF4J {@link MDC} correlation id for the duration of command handling so
 * every log line emitted downstream (validation, transactional, idempotency, handler,
 * outbox writer) carries a single trace key.
 *
 * <p>Ownership rules:
 * <ul>
 *   <li>If the key is already set (HTTP filter populated it, or the outbox poller did before
 *       re-dispatching an event), the interceptor propagates that value and does NOT clear
 *       it on exit — clearing is the outer scope's responsibility.</li>
 *   <li>If the key is absent, the interceptor generates a {@link UUID}, puts it in MDC, and
 *       removes it on exit (including when the chain throws).</li>
 * </ul>
 *
 * <p>Registered as the outermost interceptor in the chain so that validation failures and
 * rollback logs are tagged with the correlation id too.
 */
public final class CorrelationIdCommandInterceptor implements CommandInterceptor {

    @Override
    public void intercept(Command command, CommandHandlerInvoker next) {
        boolean preExisting = MDC.get(CorrelationIdMdc.KEY) != null;
        if (!preExisting) {
            MDC.put(CorrelationIdMdc.KEY, UUID.randomUUID().toString());
        }
        try {
            next.invoke(command);
        } finally {
            if (!preExisting) {
                MDC.remove(CorrelationIdMdc.KEY);
            }
        }
    }
}
