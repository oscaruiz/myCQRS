package com.oscaruiz.mycqrs.core.infrastructure.idempotency;

import java.util.UUID;

/**
 * Port for recording which commands have already been processed.
 *
 * <p>Implementations must make {@link #markProcessedIfAbsent(UUID, String)} atomic:
 * concurrent or retried invocations with the same {@code commandId} must result in
 * exactly one insertion. The single-write atomicity (e.g., Postgres
 * {@code INSERT ... ON CONFLICT DO NOTHING}) is what guarantees exactly-once semantics
 * when paired with the enclosing transaction opened by
 * {@code TransactionalCommandInterceptor}.
 */
public interface ProcessedCommandsStore {

    /**
     * Atomically records that a command is being processed.
     *
     * @param commandId   the unique identifier of this command invocation
     * @param commandType the simple class name of the command (for diagnostics only)
     * @return {@code true} if this call was the first to mark the command — the caller
     *         must proceed with the handler. {@code false} if a row already existed —
     *         the caller must skip the handler.
     */
    boolean markProcessedIfAbsent(UUID commandId, String commandType);
}
