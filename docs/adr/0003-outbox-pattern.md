# 0003 — Outbox pattern for reliable event publication

**Status:** Accepted
**Date:** 2026-04-16

## Context

Today, the flow of a successful command is:

1. `CommandBus.send()` opens a JPA transaction.
2. The handler saves the aggregate (`JpaBookRepository.save()`).
3. Transaction commits — the write model is persisted in Postgres.
4. The handler calls `eventBus.publish(event)` for every pulled domain event.
5. `SimpleEventBus` invokes each registered projection handler synchronously.

This flow has two critical gaps flagged by the architectural audit (`docs/audit-2026-04-14.md`):

- **Finding C2**: `SimpleEventBus.publish()` catches handler exceptions, logs them, and continues. A failed projection (e.g., Mongo unavailable during a create) leaves the read model out of sync with no retry and no operator signal. The command returns success to the client while the system is silently inconsistent.

- **Dual-write risk**: steps 3 and 4 are not atomic. If the process crashes between the JPA commit and the event publication, the write model has the change but no event is ever published. The read model stays stale forever, with no mechanism to recover.

Both problems compound in production. The current behavior is "best effort with silent loss" — acceptable for a lab iteration but incompatible with the framework's stated intent of explicit guarantees.

## Decision

Implement the Outbox pattern using Postgres (the same store as the write model) as the outbox store.

### Flow after this change

1. Command handler saves the aggregate within the JPA transaction.
2. **In the same transaction**, an `OutboxWriter` serializes each pulled domain event to JSON and inserts a row into an `outbox` table.
3. Transaction commits atomically: either both the aggregate and the outbox entries are persisted, or neither is.
4. A periodic `OutboxPoller` reads unpublished rows from the outbox, publishes them to the in-process event bus, and marks them as processed on success.
5. Failed publications leave the row with incremented `attempts` and `last_error` for the next poll to retry.

### Schema of the `outbox` table

| Column | Type | Notes |
|---|---|---|
| `id` | UUID, PK | Event ID, equals the `eventId` field from `DomainEvent`. |
| `aggregate_id` | VARCHAR(36) | String form of the aggregate UUID for indexing/querying. |
| `event_type` | VARCHAR(255) | Fully qualified or simple class name of the event. |
| `payload` | TEXT | JSON-serialized event body. ANSI-compliant (no JSONB) to work on H2 and Postgres alike. |
| `occurred_at` | TIMESTAMP | From the domain event. |
| `processed_at` | TIMESTAMP, NULL | Set when the poller successfully publishes. NULL means pending. |
| `attempts` | INT, default 0 | Incremented on each failed publication. |
| `last_error` | TEXT, NULL | Last error message, for operator visibility. |

Index on `(processed_at, occurred_at)` to make "pending rows in order" the fast path for the poller.

### Alternatives considered and rejected

- **Two-phase commit (XA) between Postgres and Mongo.** Rejected. Mongo's XA support is immature, XA imposes operational overhead (transaction coordinator), and the performance cost is significant for a problem Outbox solves without distributed transactions.

- **Kafka with transactional producer.** Rejected. No Kafka exists in the project. Introducing Kafka solely to solve this would require a broker, Zookeeper/KRaft, Kafka Connect, and schema registry — vastly disproportionate to the scope. Outbox is the correct prerequisite for any future Kafka adoption: if Kafka arrives later, the outbox becomes the source for a CDC connector or a transactional outbox publisher.

- **Change Data Capture via Debezium on the aggregate table directly.** Rejected for now. CDC on the aggregate table would require Debezium + Kafka Connect infrastructure. Outbox polling achieves the same guarantee with dramatically simpler operations. Documented as a future migration path if latency of polling becomes a constraint.

- **Eventual consistency without outbox (i.e., the current behavior).** Rejected. It is exactly the bug being solved. The audit finding C2 is the rejection argument.

- **Transactional outbox with listener on commit (Spring `TransactionSynchronization`).** Considered. Using `afterCommit` callbacks to publish events avoids the poller complexity. Rejected because it does not survive process crashes: if the JVM dies between commit and the `afterCommit` callback firing, the event is lost. The polling approach is robust to crashes by construction — the outbox row is the durable record.

## Consequences

### Positive

- Write model and event publication are atomic from the domain's perspective. No more silent loss.
- Failed projections do not cause command failure; they cause retry visibility. Operator can query `SELECT * FROM outbox WHERE attempts > N` to see stuck events.
- Closes audit finding C2 when Day 8 is complete.
- Enables future migration to a message broker (Kafka, RabbitMQ) without changing the command/handler contract: the poller becomes a publisher to the broker.

### Negative / pending

- Latency between command success and read model update increases by up to `poll-interval`. For interactive use cases this is acceptable; for strict read-after-write guarantees, clients must query the write side or use a read-your-writes strategy. Documented in the README.

- Guarantees become **at-least-once** for event delivery. Projections and audit handlers must be idempotent. This is Day 8 work — see the Day 8 commit for the idempotency implementation across `BookUpdatedMongoProjection`, `BookDeletedMongoProjection`, and the three audit projections.

- Operational complexity increases: one more scheduled process (the poller), one more table to monitor, one more failure mode to understand. Mitigated by keeping the poller in-process with Spring `@Scheduled` — no new infrastructure.

- Outbox table grows unbounded if not pruned. A retention policy (delete rows with `processed_at < now() - N days`) is pending and will be addressed as operational work, not in this design.

## Related work

- Day 7 (`feat/outbox-writer`): `OutboxWriter` and integration with command handlers.
- Day 8 (`feat/outbox-poller`): Poller, idempotency, reconnection of projections.
- Future: retention policy, metrics on outbox lag, potential migration to Kafka/CDC.
