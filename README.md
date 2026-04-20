# myCQRS

**A framework-agnostic CQRS core, with Spring Boot as the reference adapter.**

![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![CI](https://github.com/oscaruiz/myCQRS/actions/workflows/ci.yml/badge.svg?branch=main)
![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)

## What is this

A CQRS framework where the core — command/query/event buses, handler registration, interceptors — has zero dependencies on any DI container. Spring is wired in as an interchangeable adapter under `core.infrastructure.spring`; swapping it for Micronaut, Quarkus, or plain `new` requires no changes to `core.contracts` or `core.ddd`. On top of that, a Book bounded context demonstrates the framework end-to-end: hexagonal architecture, PostgreSQL write side with Flyway, MongoDB read side, and an outbox pattern solving the dual-write problem between them.

The codebase is designed to be read: every architectural choice is small enough to explain in an interview, and the ones that aren't obvious are documented as ADRs.

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

The outbox solves the dual-write problem. The aggregate row and the event envelope are written in the same PostgreSQL transaction — either both commit or neither does. A scheduled poller drains the outbox, publishes each event through an in-memory bus, and Mongo projectors update the read model. No distributed transaction, no lost events, no ordering gap between "event published" and "state committed".

## Modules

- **`src/core`** — reusable CQRS framework. Published as a jar; Spring is an `<optional>` dependency, not transitive. **`core.contracts` and `core.ddd` have zero Spring imports — verified by ArchUnit.**
  - `core.contracts` — ports: `Command`, `CommandBus`, `CommandHandler`, `CommandInterceptor`, `Event`, `EventBus`, `EventHandler`, `Query`, `QueryBus`, `QueryHandler`.
  - `core.ddd` — `AggregateRoot<ID>`, `DomainEvent`.
  - `core.infrastructure.bus` — in-memory `SimpleCommandBus`, `SimpleQueryBus`, `SimpleEventBus`.
  - `core.infrastructure.spring` — the Spring adapter: `@EnableCqrs`, `CqrsConfiguration`, `BeanPostProcessor`s that auto-register handlers, `ValidationCommandInterceptor`, `TransactionalCommandInterceptor`.
- **`src/demo`** — Book bounded context in hexagonal style. PostgreSQL + Flyway on the write side, MongoDB on the read side, outbox poller in between.

## Stack

- Java 21, Spring Boot 3.2.5, Maven.
- PostgreSQL + Flyway on the write side, `ddl-auto=validate`.
- MongoDB for the read model.
- JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL + MongoDB), ArchUnit.
- Docker (multi-stage build), GitHub Actions CI.

## How to run it

Prerequisites: Java 21, Docker Desktop, and the bundled Maven wrapper (`mvnw.cmd`).

```powershell
# Start PostgreSQL and MongoDB in containers
docker compose -f src\demo\docker-compose.yml up -d postgres mongo

# Run the demo application (dev profile; Flyway applies V1/V2/V3 on boot)
.\mvnw.cmd spring-boot:run -pl src/demo
```

The API uses client-generated UUIDs: the client picks the identifier, `PUT` creates the resource at that URI.

```powershell
$UUID = "550e8400-e29b-41d4-a716-446655440000"

# Create
curl.exe -X PUT "http://localhost:8080/books/$UUID" `
  -H "Content-Type: application/json" `
  -d '{\"title\":\"The Art of War\",\"author\":\"Sun Tzu\"}'

# Read (served from the Mongo projection, populated by the outbox poller)
curl.exe "http://localhost:8080/books/$UUID"

# Update
curl.exe -X PATCH "http://localhost:8080/books/$UUID" `
  -H "Content-Type: application/json" `
  -d '{\"title\":\"The Art of War (revised)\",\"author\":\"Sun Tzu\"}'

# Delete
curl.exe -X DELETE "http://localhost:8080/books/$UUID"
```

`GET /books?title=…` looks a book up by title in the read model.

## How to run the tests

```powershell
.\mvnw.cmd verify
```

Core tests run on JUnit + Mockito + AssertJ with no Spring context. Demo integration tests boot `@SpringBootTest` against Testcontainers (PostgreSQL + MongoDB) wired via `@ServiceConnection`; H2 is not used anywhere. ArchUnit enforces package boundaries in CI: contracts and ddd must not depend on Spring, the Book context must follow an onion shape, command handlers must not call each other directly, and no module may contain a slice cycle.

## Design decisions

Significant decisions — including deliberate non-adoptions such as Event Sourcing — are documented as ADRs in [`docs/adr/`](docs/adr/). The README intentionally does not summarize them; open the directory when a specific choice matters.

## Status and roadmap

**Implemented**
- Framework-agnostic core with an explicit Spring adapter (`@EnableCqrs`, deferred import, `@ConditionalOnMissingBean` on every default bean).
- Handler auto-registration via `BeanPostProcessor`s for commands, queries, and events.
- Chainable command interceptors in a fixed order: **validation → transaction → handler**. `TransactionalCommandInterceptor` uses `PlatformTransactionManager` with `PROPAGATION_REQUIRED` and commits outside the `try` block so a failing commit cannot trigger an invalid rollback.
- Outbox pattern: `OutboxEventBus` (marked `@Primary`) writes events to the outbox table inside the aggregate's transaction; `OutboxPoller` drains the table asynchronously and dispatches to the internal in-memory bus, where Mongo projectors subscribe.
- Optimistic locking on the aggregate via `@Version` on the JPA entity; `GlobalExceptionHandler` maps domain and infrastructure exceptions to meaningful HTTP status codes (e.g. `ObjectOptimisticLockingFailureException` → 409).
- Client-generated UUIDs; `PUT`/`PATCH`/`DELETE` for writes and `GET` for reads.
- Flyway migrations with `ddl-auto=validate` in every environment.
- ArchUnit enforcement of architectural boundaries in both modules.
- Testcontainers (PostgreSQL + MongoDB) for every integration test.
- Spring profiles for `dev` and `test`.
- Docker multi-stage image and GitHub Actions CI running `./mvnw verify`.

**Planned**
- Idempotency: command deduplication on the write side and projection idempotency on the read side.
- End-to-end correlation ID propagated from HTTP request through command, event, and projection.
- `demo-vanilla` adapter demonstrating the `core` jar running under a different DI container — or none at all.

## License

GPL-3.0. See [LICENSE](LICENSE).
