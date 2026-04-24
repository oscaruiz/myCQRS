# 13. Vanilla adapter demonstrating framework-agnostic core

Date: 2026-04-23

## Status

Accepted

## Context

The `core` module was designed with a deliberate constraint: zero imports of
`org.springframework.*` in `core.contracts` and `core.ddd`. The Spring-specific
infrastructure lives under `core.infrastructure.spring` and is declared as
`<optional>true</optional>` in the Maven dependency, so consumers that do not
want Spring do not get it transitively.

Until now this claim of framework-agnosticism was an assertion. The only
runnable demo (`src/demo`) bootstraps through Spring Boot — any reader or
interviewer has to take the claim on faith. There was no executable proof that
the buses, handlers, and aggregates work outside of a Spring container.

## Decision

Create a second Maven module, `src/demo-vanilla`, that consumes `core` using
only Java standard library and Javalin (a minimal HTTP server):

- **No Spring**: `mvn dependency:tree -pl src/demo-vanilla` returns no
  `org.springframework.*` line.
- **Manual bootstrap**: `VanillaBootstrapper` wires buses, handlers, and event
  projections with plain `new` — no DI container, no component scan.
- **In-memory adapters**: `InMemoryOrderRepository` and
  `InMemoryOrderReadModel` replace JPA/Postgres and MongoDB. No ORM, no
  migrations, no transactions.
- **Inline event publication**: handlers call `order.pullDomainEvents().forEach(eventBus::publish)`
  directly, because there is no transaction coordinator to hook into.
- **Separate bounded context (Order vs. Book)**: avoids extracting a
  `demo-shared-domain` module that would couple the two demos artificially.

The module follows the same hexagonal layer rules as `src/demo`:
`domain` → `application` → `infrastructure`, with `OrderReadModel` defined as
a port interface in the application layer so that `FindOrderQueryHandler` never
imports from infrastructure.

## Consequences

**Positive:**
- The framework-agnostic claim becomes a runnable proof: clone the repo,
  `mvn test -pl src/demo-vanilla`, no Docker, no Postgres.
- Establishes a repeatable bootstrap pattern for future adapters (Micronaut,
  Quarkus, plain CLI).
- Strong interview artifact: demonstrates architectural intentionality, not
  just Spring fluency.

**Negative:**
- One additional module to maintain. If a `core` interface changes (e.g.,
  `CommandBus.registerHandler` signature), both demos must be updated.
- The Order domain duplicates structural patterns from the Book domain. This is
  deliberate; sharing domain code between demos would introduce coupling that
  obscures the message.

## Alternatives considered and rejected

**Unit tests of core in isolation.** Rejected: a unit test exercises the bus
contracts but does not prove that the core can be bootstrapped with a real HTTP
entry point and in-memory adapters without Spring. The value is the integration
of all pieces without a container.

**Same bounded context (Book) in both demos.** Rejected: would require either
duplicating domain classes verbatim or extracting a `demo-shared-book-domain`
module. Both options add noise that distracts from the framework-agnostic
message.

**No HTTP layer — only a `main()` that runs commands programmatically.** Rejected:
omitting Javalin would reduce demo-vanilla to a test wrapper. An HTTP adapter
demonstrates that the core works in a realistic request/response lifecycle, not
just in a test harness.

**Using Javalin in `src/demo` as well (unify the HTTP layer).** Rejected:
`src/demo` is the Spring Boot demo. Replacing Spring MVC with Javalin there
would destroy the simetry that makes the comparison meaningful — we want
`src/demo` to be idiomatic Spring Boot and `src/demo-vanilla` to be idiomatic
plain Java.

**Pessimistic event publication (interceptor-based).** Rejected: without a
real transaction manager there is no transaction boundary to hook into.
Inline publication is the correct model for in-memory adapters; it is
explicitly supported by the same `pullDomainEvents()` API that the Spring demo
uses, just called from a different site.

## Related work

- ADR 0001: core package structure — the optional Spring dependency model that
  makes this separation possible.
- ADR 0005: event sourcing rejected — documents why the domain model does not
  need to track version or event history, which keeps `OrderAggregate` simple.
