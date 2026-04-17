# 0004 — Optimistic locking on the write model via JPA `@Version`

**Status:** Accepted
**Date:** 2026-04-17

## Context

Before this decision, the `PATCH /books/{id}` flow had a silent lost-update
vulnerability. Two concurrent requests to the same book would:

1. Both load the same `BookEntity` state (same `title`, `author`, `deleted`).
2. Both mutate the aggregate through `UpdateBookCommandHandler`.
3. Both call `JpaBookRepository.save()`.

The second writer's `UPDATE` overwrites the first writer's change with no
detection and no signal to the client. Both HTTP responses return `200 OK`.
The system is silently inconsistent with the client's mental model — the
client who wrote first never sees that their change was thrown away.

`AGENTS.md` flagged this as a pending requirement, and the README listed it
under "Future improvements". The architectural audit considered it latent
data-integrity risk — harmless with a single client, but real data loss
as soon as there is concurrency.

The `TransactionalCommandInterceptor` already opens one JPA transaction per
`commandBus.send(...)`, so concurrent commands already run in independent
transactions. The missing piece was a mechanism inside the transaction to
reject a write that targets a stale version of the row.

## Decision

Introduce optimistic locking at the persistence layer using JPA's `@Version`
annotation on `BookEntity`.

### Changes

- Add `@Version private Long version` to `BookEntity`. JPA manages the field
  by reflection — no getter, no setter, no constructor argument.
- Flyway migration `V3__add_book_version.sql` adds the column to existing
  databases with a safe default:
  ```sql
  ALTER TABLE book_entity ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  ```
- `GlobalExceptionHandler` maps `ObjectOptimisticLockingFailureException`
  to `HTTP 409 Conflict` with an `ApiError` body carrying the message
  "The resource was modified by another request. Please retry.".
- `BookAggregate` is **not** modified. `rehydrate()` does not receive a
  version, and `JpaBookRepository.toAggregate()` does not read the column.

### Where the `version` field lives

The version field lives in `BookEntity` (infrastructure), not in
`BookAggregate` (domain). Optimistic locking is a persistence concern: the
mechanism exists to detect that the row we are about to `UPDATE` is no longer
the one we read. The domain has no behavior that depends on knowing "what
version am I?" today. YAGNI.

If a future requirement raises this concern into the domain — event sourcing
with versioned events, conditional HTTP requests exposing `ETag`/`If-Match`,
or explicit concurrency tokens at the application boundary — the field is
promoted to the aggregate at that point. Not before.

### Exception propagation path

When Hibernate detects the version mismatch on commit:

```
jakarta.persistence.OptimisticLockException
  → Spring wraps in ObjectOptimisticLockingFailureException
    → bubbles out of JpaBookRepository.save()
      → out of the command handler
        → TransactionalCommandInterceptor rolls back
          → SimpleCommandBus.send() re-throws
            → BookController handler method
              → GlobalExceptionHandler → HTTP 409
```

The rollback is essential: without the outbox row also rolling back, the
read model would project an update the write model never committed.

### What this means for the client

An HTTP `409 Conflict` on a `PATCH` means "your view of the resource was
stale". The expected client behavior is:

1. `GET /books/{id}` to re-read the current state.
2. Re-compute the desired change against fresh data.
3. Retry the `PATCH` with the updated view.

Retry is **not automatic**. The server cannot know whether the second
writer's intent is still valid against the new state — that judgment
belongs to the client.

### Alternatives considered and rejected

- **Pessimistic locking (`SELECT ... FOR UPDATE`).** Rejected. Pessimistic
  locks hold database row-level locks for the duration of the request. Under
  even moderate concurrency, HTTP threads queue on locks, tail latency
  degrades, and deadlocks become possible when multi-row writes arrive in
  different orders. Optimistic locking is the cheaper default for HTTP
  workloads where conflicts are the exception, not the rule.

- **Field-level merge strategies.** Rejected for now. A merge strategy would
  let two concurrent writers succeed when they touch disjoint fields (one
  writer changes `title`, another changes `author`). This is a richer
  semantics than "first writer wins", but it requires a merge resolver per
  aggregate and opens questions about field-level authorship and audit.
  Not in scope for this iteration. If the use case appears, it composes on
  top of optimistic locking rather than replacing it.

- **Compare-and-swap with HTTP `ETag` / `If-Match`.** Rejected for now.
  CAS over HTTP pushes the version token to the client, which is the correct
  design for a public API but duplicates work we do not need yet: there is
  no public client, the API is single-tenant internal, and OpenAPI wiring
  for `ETag` is a Day 16+ concern. Optimistic locking at the persistence
  layer solves the immediate lost-update problem with a server-only change.
  Adding `ETag` later is additive and does not invalidate this decision.

- **Application-level `version` check inside the command handler.**
  Rejected. The handler would need to load the aggregate, compare an
  expected version against the stored one, then `save()`. This duplicates
  what `@Version` does atomically inside the `UPDATE` statement and opens
  a TOCTOU gap between the check and the save. JPA's implementation is
  already atomic at the SQL level (`UPDATE ... WHERE id=? AND version=?`);
  no application-layer check can be tighter.

- **Do nothing and rely on low concurrency.** Rejected. It is the bug being
  fixed. Silent data loss under concurrency is exactly the class of failure
  the framework is designed to rule out, not tolerate.

## Consequences

### Positive

- Lost updates on `PATCH /books/{id}` are detected and reported as
  `HTTP 409 Conflict` instead of silent overwrites. "First writer wins"
  is now enforced, not just assumed.
- The mechanism is enforced at the row level inside a single SQL `UPDATE`,
  so it is correct even under the outbox pattern: a conflict rolls back
  the aggregate write and the outbox row together atomically.
- Integration tests prove both the sequential case (two detached entities
  with stale versions) and the concurrent case (two threads sending
  `UpdateBookCommand` against the same aggregate through the command bus
  with a `CountDownLatch` barrier).
- The domain stays free of a persistence concern. `BookAggregate` remains
  a pure domain object; clients of the aggregate port do not need to know
  that a version column exists.

### Negative / pending

- Clients must handle `HTTP 409` explicitly. There is no automatic retry on
  the server side. Interactive UIs need to surface the conflict and guide
  the user through re-reading and re-submitting. This cost is deliberate:
  an automatic retry would hide the conflict we just made visible.
- Semantic conflicts — two concurrent writers editing different fields of
  the same aggregate, each individually valid but incompatible together —
  are **not** resolved. The second writer gets a 409 even though a merge
  was theoretically possible. Accepted trade-off; see the rejected merge
  alternative above.
- `@Version` detects conflicts at commit time, so it does not prevent the
  work done inside the losing transaction. Under high-contention workloads,
  the rejected transaction wastes CPU, database connections, and outbox
  inserts that end up rolled back. If contention becomes a bottleneck, the
  answer is either coarser serialization (queue per aggregate) or redesign
  of the aggregate boundary, not a different locking strategy.
- The integration test for real concurrency (`CountDownLatch`) is
  timing-dependent by construction. It passes reliably on H2 today but is
  a candidate for flakiness on slower CI hardware. `PRD.md` notes the
  expectation to re-verify it on Testcontainers Postgres at Day 11.

## Related work

- Day 7–8 (`feat/outbox-*`): ADR 0003. The outbox row participates in the
  same JPA transaction as the aggregate `UPDATE`, so a version conflict
  rolls both back atomically.
- Day 11 (planned): replace H2 with Testcontainers Postgres. Remove `version`
  from `schema.sql` when the file is deleted. Re-run the concurrency test
  against real Postgres to confirm timing behavior.
- Day 16+ (candidate): expose version through HTTP `ETag` / `If-Match` as
  part of OpenAPI publication. That decision, if taken, will be documented
  in a follow-up ADR and builds on this one rather than replacing it.
