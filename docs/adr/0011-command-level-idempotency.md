# 0011 — Command-level idempotency

**Status:** Accepted
**Date:** 2026-04-22

## Context

The outbox pattern (ADR 0003) gives **at-least-once** delivery for events
published by the write side. Until this decision, the write side itself had
no symmetric protection: commands crossing into `commandBus.send(...)` would
execute their full side-effect chain on every invocation, with nothing to
distinguish a genuine new operation from a retry of the same one.

Duplicate invocations are not hypothetical. Three realistic paths produce
them today or in the near future:

1. **HTTP client retries.** A client receiving an ambiguous timeout (request
   sent, response lost) retries. The server runs the write twice.
2. **Outbox-driven downstream consumers.** An event projected elsewhere can
   trigger a follow-up command. Outbox at-least-once semantics mean that
   event may be delivered more than once, producing more than one command.
3. **Internal re-dispatch during incident recovery.** Replaying a poisoned
   outbox message can re-fire the command the operator intended to skip.

Each of these turns "exactly once" on the business outcome into
"unpredictable how many times" on the state transition. For a soft-delete or
a balance debit, that is silent data corruption.

The `TransactionalCommandInterceptor` already opens one JPA transaction per
`commandBus.send(...)`. What was missing was a single-write atomic record —
inside that same transaction — proving that a specific command invocation
had already been processed, so that the handler could be skipped on the
second arrival.

## Decision

Every `Command` carries a client-scoped `UUID commandId()`. An
`IdempotencyCommandInterceptor` registered as the innermost interceptor
writes to a `processed_commands` ledger with
`INSERT ... ON CONFLICT (command_id) DO NOTHING`. If the insert affected a
row, the handler runs; if not, the invocation is a duplicate and is silently
skipped.

### Changes

- `Command` gains an abstract `UUID commandId()`. Every concrete command in
  the demo is migrated. No marker interface.
- New `processed_commands` table (Flyway `V6__processed_commands.sql`):
  ```sql
  CREATE TABLE processed_commands (
      command_id   UUID         NOT NULL,
      command_type VARCHAR(255) NOT NULL,
      processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      PRIMARY KEY (command_id)
  );
  CREATE INDEX idx_processed_commands_processed_at ON processed_commands (processed_at);
  ```
- New port `ProcessedCommandsStore` in `core.idempotency` with a single
  atomic method `markProcessedIfAbsent(UUID, String) → boolean`.
- Spring-aware adapter `JdbcProcessedCommandsStore` in
  `core.infrastructure.spring.idempotency` uses
  `NamedParameterJdbcTemplate.update(...) == 1` to detect first-insert vs.
  duplicate.
- `CqrsConfiguration.commandBus(...)` registers
  `IdempotencyCommandInterceptor` last. `SimpleCommandBus` wraps interceptors
  in reverse registration order, producing execution order
  `Validation → Transactional → Idempotency → Handler`.

### Where the `commandId` comes from

Today, HTTP controllers generate `UUID.randomUUID()` at each entry point.
This means retries initiated by the HTTP layer itself get a fresh `commandId`
and **are not de-duplicated**. Idempotency protects only invocations that
reuse an existing `commandId`: internal re-dispatch paths, future downstream
consumers reacting to outbox events, and tests. See _Negative / pending_.

### Why the interceptor sits inside the transaction

The critical property is that the ledger insert and the handler's side
effects commit or roll back together. If the ledger insert committed in its
own transaction ahead of the handler, and the handler then failed, the
retry would see an existing row and silently skip — the command would have
recorded "processed" without any effect actually happening. Putting the
interceptor inside the `Transactional` one closes this window: a handler
exception triggers `PlatformTransactionManager.rollback(status)`, which
reverses the ledger insert along with everything else.

### Why `INSERT ... ON CONFLICT DO NOTHING`

Postgres aborts the current transaction on a primary-key violation.
`try/catch DuplicateKeyException` inside the interceptor would therefore
poison the handler's transaction — the caller would need savepoints to
recover, which is strictly more complex than a single conflict-aware
statement. `ON CONFLICT DO NOTHING` makes duplicate detection a normal
execution path that returns zero affected rows; the transaction stays
clean and commits (emptily) if the handler is skipped.

### Alternatives considered and rejected

- **Marker interface `IdempotentCommand` (opt-in).** Rejected. Mutating
  commands should be idempotent by default. An opt-in split creates two
  tiers with different safety guarantees, and every new command requires a
  judgment call the implementer can get wrong silently. Making `commandId`
  part of the base contract removes the class of mistake.

- **Interceptor before the transaction, with a separate insert
  transaction.** Rejected. Non-atomic with handler side effects. Between
  "marked processed" committing and the handler's tx committing, a handler
  failure leaves a ledger row without any work to match it. The next retry
  observes the ledger row and skips — a silent no-op for a command that
  never actually happened. Inside the same transaction, both survive or
  both die together.

- **`try/catch DuplicateKeyException` with a plain `INSERT`.** Rejected.
  In Postgres, a PK violation aborts the entire current transaction:
  subsequent statements fail with `current transaction is aborted`. Recovery
  requires savepoints bracketing every insert, which is strictly more
  complex than `ON CONFLICT DO NOTHING` and buys nothing.

- **Storing the commandId on the aggregate / on outbox rows.** Rejected.
  Coupling idempotency to aggregate state or to the event pipeline would
  tie the mechanism to one bounded context and one delivery path. A
  cross-cutting concern belongs in a cross-cutting interceptor, not in
  business schema.

- **Do nothing — rely on handlers to be idempotent by hand.** Rejected.
  That is the bug being fixed. Soft-deletes and state transitions already
  in the demo are not naturally idempotent: a retry of `DeleteBookCommand`
  on an already-deleted aggregate throws, and a retry of an update mutates
  twice. Relying on every future handler author to re-derive idempotency
  discipline is exactly the class of latent failure the framework exists
  to rule out.

## Consequences

### Positive

- Re-delivery of the same command — by any route, HTTP-layer or internal —
  produces exactly one side effect. The outbox narrative is now complete:
  at-least-once in the pipeline, exactly-once at the side effect.
- The guarantee is atomic with the handler. A failing handler rolls back
  its ledger row along with everything else, so a subsequent retry behaves
  like a first try rather than a silent skip. This is validated by
  `CommandIdempotencyIntegrationTest#handlerFailure_rollsBackProcessedCommandsRow`.
- The atomicity claim is verified by
  `CommandIdempotencyIntegrationTest#retryAfterHandlerFailure_reExecutesHandler_andMarksOnce`,
  which asserts both SQL-level rollback and behavioral re-execution on retry:
  after a failed first invocation, a second send with the same `commandId`
  runs the handler again, persists the aggregate, and leaves exactly one
  ledger row. The test binds this ADR to its behavioral guarantee — if the
  test is ever weakened or deleted, the reference surfaces it in review.
- The contract lives on `Command` itself. Every future command has to
  declare `commandId()`; there is no opt-in path that can be forgotten.
- The `core.idempotency` package depends on no Spring classes. The Spring
  adapter is segregated in `core.infrastructure.spring.idempotency`. A new
  ArchUnit rule (`idempotencyDoesNotDependOnSpring`) enforces this going
  forward.

### Negative / pending

- **HTTP-level retries are not de-duplicated today.** Controllers generate
  a fresh `commandId` per inbound request, so a client retrying the same
  `PATCH` after a timeout produces two distinct commandIds and two state
  transitions. Closing this gap requires clients to supply a stable
  idempotency key and controllers to propagate it into `commandId`. The
  conventional form is an `Idempotency-Key: <uuid>` HTTP header, with the
  server either requiring it or defaulting to a server-side random when
  absent. This is a follow-up ADR, planned alongside OpenAPI publication
  (Day 16+).
- **`processed_commands` grows unbounded.** No TTL job exists. The
  `idx_processed_commands_processed_at` index is in place to make the
  eventual delete cheap (candidate policy: rows older than 30 days).
  Deferred — the cost of an unbounded but narrow append-only table is not
  yet a bottleneck.
- **Result-returning commands are not supported.** Both `CommandHandler.handle`
  and `CommandBus.send` return `void`, and the interceptor's skip branch
  returns nothing. If a future command produces a caller-visible result
  (e.g., a generated id), exactly-once delivery of that result requires
  caching the result on the ledger row and replaying it on duplicate —
  which this design does not do. Current handlers are void, so the gap is
  theoretical, but it is the reason to keep commands void unless there is
  a strong contrary pressure.
- **Byte-identical retries only.** The contract is "same operation implies
  same `commandId`". Two commands carrying different `commandId`s with
  otherwise identical payloads will both run. The system cannot protect
  against a client that generates a new key for what is semantically the
  same request — the client owns that responsibility.
- **One extra row per command.** Every write path pays for an additional
  `INSERT`. The cost is negligible at current scale; it is a real cost at
  millions of commands per minute and is worth measuring if that day
  arrives.

## Related work

- ADR 0003 (outbox pattern). The outbox row, the aggregate `UPDATE`, and
  now the `processed_commands` row all commit in the same JPA transaction.
  This ADR is the third leg of that atomicity triangle.
- ADR 0004 (optimistic locking). Orthogonal. `@Version` conflicts fire
  before the transaction commits; idempotency checks fire before the
  handler runs. A `409 Conflict` on the wire means "you wrote against
  stale state"; a silent skip means "you sent the same command twice".
  The two mechanisms do not overlap.
- **Follow-up (planned): ADR 0012 — correlation ID.** Separate concern;
  propagates a trace identifier through the pipeline for observability.
  Not a dependency of this ADR.
- **Follow-up (planned, Day 16+): `Idempotency-Key` HTTP header.** Closes
  the HTTP-level retry gap documented above. Will build on this ADR rather
  than replace it.
