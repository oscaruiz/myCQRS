package com.oscaruiz.mycqrs.demovanilla.infrastructure.interceptor;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCommandInterceptor implements CommandInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingCommandInterceptor.class);

    @Override
    public void intercept(Command command, CommandHandlerInvoker next) {
        String name = command.getClass().getSimpleName();
        long start = System.currentTimeMillis();
        log.debug("COMMAND → {}", name);
        try {
            next.invoke(command);
            log.debug("COMMAND ← {} ({}ms)", name, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.debug("COMMAND ✗ {} ({}ms) — {}", name, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }
}
