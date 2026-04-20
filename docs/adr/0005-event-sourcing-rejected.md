# ADR 0005: Event Sourcing considered and rejected for `demo`

## Status

Accepted — 2026-04-20

## Context

This project is an architectural learning vehicle: a reusable CQRS `core` framework and a `demo` bounded context that consumes it. The `Book` domain is deliberately simple (CRUD with few invariants) and is not justified by real business requirements — not even CQRS is. The relevant architectural question is therefore not *"what best solves my problem?"* but *"which patterns do I want to understand and demonstrate in depth?"*.

Within that framing, Event Sourcing (ES) frequently appears in the literature as a natural extension of CQRS (Axon, Codely, blog posts). Before adopting or rejecting it, it is evaluated against real-project criteria.

### Prior conceptual distinction

The current system is **event-driven**, not **event-sourced**. These are orthogonal axes:

- **Event-driven (current):** aggregate state is persisted in Postgres tables (`books`). `DomainEvent`s are published via the `EventBus` and projected asynchronously to Mongo through the Outbox pattern. Events are *notifications* of a fact already committed; deleting them does not destroy state.
- **Event-sourced (rejected):** an append-only `EventStore` would be the source of truth. Aggregate state would be reconstructed with `AggregateRoot.loadFromHistory(events)` on every operation. Without the events, the aggregate ceases to exist.

CQRS does not imply ES. Their joint adoption in the literature reflects specific domains (financial, regulatory traceability), not an architectural dependency.

### ES adoption criteria evaluated

ES would be justified if any of the following held:

1. **Temporal queries as a functional requirement** (*"what was the state on 3/3 at 14:32?"*). Does not apply to `Book`.
2. **Regulatory auditing** (PSD2, HIPAA, SOX). Does not apply.
3. **Behavior is the product** — analysis of how users arrive at state, not just final state (shopping carts, customer journeys). Does not apply.
4. **Operational projection repair** as a recurring need. The current Mongo projection can be rehydrated directly from `books`; there is no independent justification.

Zero of the four criteria apply to the domain.

## Decision

Event Sourcing is rejected for `demo`. The write model remains relational state in Postgres; `DomainEvent`s remain event-driven notifications delivered via Outbox to Mongo projections.

Consequently, `core` **exposes no ES abstractions** (`EventStore`, `AggregateRoot.loadFromHistory`, snapshots). If a separate bounded context whose domain genuinely justifies ES is built in the future (e.g. a bank account), the abstractions will emerge from that concrete work (Rule of Three), not from anticipatory design.

## Consequences

### What becomes easier

- **Simpler operations:** no `EventStore`, no snapshots, no compaction job, no event-version management in production.
- **Schema evolution:** modifying the `books` schema is an `ALTER TABLE` via Flyway. With ES it would require maintaining `BookCreated.v1`, `BookCreated.v2`, and upcasters between versions for the entire lifetime of the system.
- **Debugging:** current state is inspected with `SELECT`. Without ES there is no need to replay history to understand a bug.
- **Onboarding:** any developer with Spring/JPA understands the write side. ES requires specific training.

### What becomes harder

- **Temporal queries:** the system cannot answer *"what was the state on day X?"* without an explicit audit log. Acceptable: not a requirement.
- **Granular projection rebuild:** if Mongo becomes corrupted, it is rehydrated from `books`, not from an operation-by-operation event log. Acceptable for a CRUD domain.
- **"Why" traceability:** we capture *what* changed (via domain events persisted in the Outbox), but not always *the command that caused it* or prior state, unless explicitly modeled. Acceptable given no regulatory requirement.

### Explicit technical debt

Migrating `demo` to ES retroactively would be costly: existing aggregates in `books` have no history, forcing synthetic `BookImportedFromLegacy` events that pollute the stream from its origin, and schema evolution becomes a problem from day one. The path to explore ES, therefore, **is not to migrate this module**, but to build a new one from scratch with a domain where ES is genuinely justified.

## Alternatives considered

- **Full ES in `demo`:** rejected on the criteria above.
- **Hybrid ES (relational state + parallel event log as secondary source):** rejected. Duplicates infrastructure without delivering the benefits of real ES (no `loadFromHistory`; the log is not the source of truth). Accumulates the costs of both approaches without gaining either.
- **Adopt Axon Framework:** rejected. Reusing Axon teaches how to *use* the framework, not how to understand ES at the implementation level — contrary to the project's pedagogical goal.
- **Outbox + event-driven read models (accepted — current implementation):** solves the dual-write problem, provides basic traceability via persisted events, and preserves the simplicity of the relational write model.

## References

- Fowler, M. *CQRS* — <https://martinfowler.com/bliki/CQRS.html>
- Young, G. *CQRS Documents*
- Vernon, V. *Implementing Domain-Driven Design*, ch. Event Sourcing
