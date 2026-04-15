# 0002 — Unified read model in MongoDB with full lifecycle projections

**Status:** Accepted
**Date:** 2026-04-15

## Context

Prior to this decision, the demo's read side had three coexisting read stores with overlapping but inconsistent state:

- `BookReadRepository`: an in-memory `HashMap<String, Book>` used by query handlers.
- `BookMongoRepository`: a Mongo collection written to by `BookCreatedMongoProjection`.
- The Postgres write model itself, indirectly readable via the JPA repository.

Only the create flow updated all three. Updates and deletes touched Postgres (write side) but never reached the in-memory store that query handlers read from. As a result, `GET /books/{id}` returned stale data after `PATCH` and ghost books after `DELETE`. The architectural audit (`docs/audit-2026-04-14.md`) flagged this as critical findings C1 and C3 — the latter compounded by query handlers returning `null` and the controller producing NPE → 500 instead of deterministic 404.

## Decision

Unify the read model on MongoDB as the single read store, accessed by query handlers through a port:

- Introduce `BookReadModelRepository` interface in `demo.application.query` as the abstraction query handlers depend on.
- Implement the port via `MongoBookReadModelRepository` in `demo.infrastructure.mongo`, which adapts `BookMongoRepository` (Spring Data) to the port contract.
- Add `BookUpdatedMongoProjection` and `BookDeletedMongoProjection` so all three lifecycle events (create, update, delete) keep the read model coherent.
- Delete the in-memory `BookReadRepository` and the duplicate `BookCreatedEventProjection` that wrote to it.
- Query handlers return through `Optional<Book>` and throw `NoSuchElementException` on empty — mapped to HTTP 404 by `GlobalExceptionHandler`.
- Read-model deletion is **hard delete** (`deleteById` on the Mongo collection).

### Alternatives considered and rejected

- **Keep the HashMap as a test double in production code.** Rejected: it had zero production purpose and obscured the production read path. If a fake is needed in tests, it lives in test sources as a private inner class.

- **Soft delete in the read model** (a `deleted` flag on `BookReadModel`). Rejected: the write model is already soft-delete (`BookEntity.deleted`), and the audit log retains every deletion event. Duplicating soft-delete state in the read model adds complexity for use cases that don't exist. If a future feature requires "list deleted books", the audit log is the source.

- **Place the port in `domain` instead of `application.query`.** Rejected: in CQRS the read model is an application-layer artifact, not a domain concept. The write side has a domain port (`BookRepository`) because aggregate persistence is a domain concern. Read models exist to serve queries; their location is application.

- **Return `Optional<Book>` from query handlers and let the controller decide.** Rejected: would split error-handling logic across handler and controller. Using `NoSuchElementException` keeps error mapping centralized in `GlobalExceptionHandler` and consistent with the rest of the pipeline.

## Consequences

### Positive

- One source of truth for reads. No more divergence between stores.
- Read model coherence is testable end-to-end with Testcontainers; the new `BookLifecycleIntegrationTest` exercises the full create → update → query and create → delete → query flows against real Mongo.
- Audit findings C1 (orphan state) and C3 (NPE on not-found) closed. Finding H4 (HashMap thread-safety) closed by removal.
- Query handlers are now testable in isolation against a fake of the port without booting any infrastructure (see `FindBookByTitleQueryHandlerTest`).

### Negative / pending

- Read availability is now coupled to Mongo availability. Previously, the HashMap always answered (with potentially wrong data); now, if Mongo is down, queries fail. This is the correct trade-off — wrong data is worse than no data — but worth noting for any future SLO discussion.

- Event publication still goes through `SimpleEventBus`, which catches and logs handler exceptions silently (audit finding C2). A failed projection after a successful command leaves the read model out of sync with no retry. This is the next critical work; see future ADR 0003 (planned: Outbox pattern).

- No event versioning yet (audit finding M4). Event payload changes would silently break projections. Acceptable for current scope; revisit before introducing event replay or external consumers.
