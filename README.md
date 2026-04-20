# myCQRS

A DI-container-agnostic CQRS core in Java 21, with Spring Boot as the reference adapter.

![CI](https://github.com/oscaruiz/myCQRS/actions/workflows/ci.yml/badge.svg?branch=main)

## What is this?

A two-module Maven project built to demonstrate CQRS, DDD, and hexagonal architecture at a senior level without hiding the mechanics behind a framework. The `core` module provides the command, query, and event buses, interceptor chain, and handler auto-registration contracts as a reusable jar; Spring dependencies are declared `<optional>` and live only in a single adapter sub-package. The `demo` module wires that core into a Book bounded context backed by PostgreSQL (write side, Flyway), MongoDB (read side and audit log), and an outbox poller for eventual consistency. The repository doubles as a sandbox for architectural decisions that are documented, not improvised — including the ones that were rejected.

## Architecture

### Write flow

```
HTTP request
    │
    ▼
Controller ──► CommandBus ──► [ Validation ─► Transaction ─► Handler ]
                                                                │
                                                                ▼
                                   Aggregate UPDATE + Outbox INSERT
                                   (single PostgreSQL transaction)
                                                                │
                                                                ▼
                                          OutboxPoller (scheduled)
                                                                │
                                                                ▼
                                          EventBus (in-memory)
                                                                │
                                                                ▼
                                          Projectors ──► MongoDB
```

### Read flow

```
HTTP request ──► Controller ──► QueryBus ──► QueryHandler ──► MongoDB
```

The outbox solves the dual-write problem. The aggregate row and the event envelope are written in the same PostgreSQL transaction, so either both commit or neither does. A scheduled poller drains the outbox, publishes each event through an internal in-memory bus, and the Mongo projectors update the read model. No distributed transaction, no lost events, no "publish first, hope the DB commits" ordering hazard.

## Modules

- **`src/core`** — reusable CQRS framework. Published as a jar; Spring is an `<optional>` dependency, not a transitive one.
  - `core.contracts` — ports: `Command`, `CommandBus`, `CommandHandler`, `CommandInterceptor`, `Event`, `EventBus`, `EventHandler`, `Query`, `QueryBus`, `QueryHandler`. **Zero Spring imports.**
  - `core.ddd` — `AggregateRoot<ID>`, `DomainEvent`. **Zero Spring imports.**
  - `core.infrastructure.bus` — in-memory `SimpleCommandBus`, `SimpleQueryBus`, `SimpleEventBus`.
  - `core.infrastructure.spring` — the Spring adapter: `@EnableCqrs`, `CqrsConfiguration`, `BeanPostProcessor`s that auto-register handlers, `ValidationCommandInterceptor`, `TransactionalCommandInterceptor`.
- **`src/demo`** — Book bounded context in hexagonal style. PostgreSQL + Flyway on the write side, MongoDB on the read side, outbox poller in between.

## Stack

- Java 21, Spring Boot 3.2.5, Maven (wrapper included).
- PostgreSQL + Flyway on the write side, `ddl-auto=validate`.
- MongoDB for the read model and the audit log.
- JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL + MongoDB), ArchUnit.
- Docker (multi-stage build), GitHub Actions CI.

## How to run it

Prerequisites: Java 21, Docker, and the bundled Maven wrapper.

```bash
# Start PostgreSQL and MongoDB in containers
docker compose -f src/demo/docker-compose.yml up -d postgres mongo

# Run the demo application (dev profile; Flyway applies V1/V2/V3 on boot)
./mvnw spring-boot:run -pl src/demo
```

The API uses client-generated UUIDs: the client picks the identifier, `PUT` creates the resource at that URI.

```bash
UUID=$(uuidgen)

# Create
curl -X PUT "http://localhost:8080/books/$UUID" \
  -H "Content-Type: application/json" \
  -d '{"title":"The Art of War","author":"Sun Tzu"}'

# Read (served from the Mongo projection, populated by the outbox poller)
curl "http://localhost:8080/books/$UUID"

# Update
curl -X PATCH "http://localhost:8080/books/$UUID" \
  -H "Content-Type: application/json" \
  -d '{"title":"The Art of War (revised)","author":"Sun Tzu"}'

# Delete
curl -X DELETE "http://localhost:8080/books/$UUID"
```

`GET /books?title=…` looks a book up by title in the read model.

## How to run the tests

```bash
./mvnw verify
```

Core tests run on JUnit + Mockito + AssertJ with no Spring context — a concrete demonstration that the core doesn't depend on the container to be exercised. Demo integration tests boot a `@SpringBootTest` against Testcontainers (PostgreSQL + MongoDB) wired via `@ServiceConnection`; H2 is not used anywhere. ArchUnit enforces package boundaries in CI: contracts and ddd must not depend on Spring, the Book context must follow an onion shape, command handlers must not call each other directly, and no module may contain a slice cycle.

## Design decisions

Significant decisions — including deliberate non-adoptions such as Event Sourcing — are documented as ADRs in [`docs/adr/`](docs/adr/). The README intentionally does not summarize them; open the directory when a specific choice matters.

## Status and roadmap

**Implemented**
- Framework-agnostic core with an explicit Spring adapter (`@EnableCqrs`, deferred import, `@ConditionalOnMissingBean` on every default bean).
- Handler auto-registration via `BeanPostProcessor`s for commands, queries, and events.
- Chainable command interceptors in a fixed order: **validation → transaction → handler**. `TransactionalCommandInterceptor` uses `PlatformTransactionManager` with `PROPAGATION_REQUIRED` and commits outside the `try` block so a failing commit cannot trigger an invalid rollback.
- Outbox pattern: `OutboxEventBus` (marked `@Primary`) writes events to the outbox table inside the aggregate's transaction; `OutboxPoller` drains the table asynchronously and dispatches to the internal in-memory bus, where Mongo projectors subscribe.
- Optimistic locking on the aggregate via `@Version` on the JPA entity; `GlobalExceptionHandler` maps `ObjectOptimisticLockingFailureException` to HTTP 409.
- Client-generated UUIDs; `PUT`/`PATCH`/`DELETE` for writes and `GET` for reads.
- Flyway migrations with `ddl-auto=validate` in every environment.
- ArchUnit enforcement of architectural boundaries in both modules.
- Testcontainers (PostgreSQL + MongoDB) for every integration test.
- Spring profiles for `dev` and `test`; Docker multi-stage image; GitHub Actions CI running `./mvnw verify`.
- `GlobalExceptionHandler` mapping domain and infrastructure exceptions to meaningful HTTP status codes.

**Planned**
- Idempotency: command deduplication on the write side and projection idempotency on the read side.
- End-to-end correlation ID propagated from HTTP request through command, event, and projection.
- `demo-vanilla` adapter demonstrating the `core` jar running under a different DI container — or none at all.

## License

GPL-3.0. See [LICENSE](LICENSE).
