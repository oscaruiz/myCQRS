package com.oscaruiz.mycqrs.core.contracts.command;

import java.util.UUID;

/**
 * Contract for all commands in the system.
 *
 * <p>Every command carries a {@link UUID} identifying this specific invocation. The id is
 * consumed by {@code IdempotencyCommandInterceptor} to guarantee exactly-once side effects
 * under at-least-once delivery (HTTP retries, outbox-driven re-dispatch, etc.).
 */
public interface Command {

    UUID commandId();
}
