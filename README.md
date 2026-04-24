# myCQRS

**A framework-agnostic CQRS core, proven by two independent adapters: Spring Boot and plain Java.**

![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![CI](https://github.com/oscaruiz/myCQRS/actions/workflows/ci.yml/badge.svg?branch=main)
![Deploy](https://github.com/oscaruiz/myCQRS/actions/workflows/deploy.yml/badge.svg?branch=main)
![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)

## What is this

A CQRS framework where the core (command/query/event buses, handler registration, interceptors) has zero dependencies on any DI container. The thesis — that `core` is framework-agnostic — is not an assertion; it is demonstrated by two independent demos consuming the same `core` jar:

- **`demo`** — a Book bounded context running on Spring Boot, PostgreSQL (write), MongoDB (read), and the outbox pattern. Hexagonal, production-shaped.
- **`demo-vanilla`** — an Order bounded context bootstrapped with plain Java. No Spring, no DI container, manual handler registration, in-memory adapters, Javalin as the only web dependency.

Swapping Spring for Micronaut, Quarkus, or plain `new` requires no changes to `core.contracts` or `core.ddd` — verified by ArchUnit rules and made executable by `demo-vanilla` (`mvn dependency:tree -pl src/demo-vanilla | grep springframework` returns empty).

The codebase is designed to be read: every architectural choice is small enough to explain in an interview, and the ones that aren't obvious are documented as ADRs.

## Live demo

`demo` is deployed on Render free tier. Swagger UI entry point:

`https://mycqrs.onrender.com/swagger-ui.html`

> Free tier; first request after idle may take ~30s. Conscious trade-off —
> production would use a paid tier or `min-instances=1`.

| Endpoint | Purpose |
|---|---|
| `PUT /books/{id}` | Creates a book (client-generated UUID). |
| `GET /actuator/outbox` | Outbox stats — watch `pending` drain to `processed`. |
| `GET /books/{id}` | Reads from the Mongo projection. |

Deployment rationale: [ADR 0007](docs/adr/0007-immutable-docker-images-via-ghcr.md).

See the dashboard at `/` for the full observability surface (commands, outbox stats, recent events, read-side projections, write↔read snapshot).

`demo-vanilla` is not deployed — it's designed to run locally (`mvn exec:java`) and in tests. Its value is the proof of portability, not a production surface.

## Dashboard

Served at `/` by the running `demo` deployment. Single-page HTML (no framework, no build
step) that exposes the full CQRS pipeline end-to-end in one view:

- **Write side:** create/update/delete books and authors, link an author to
  a book, and see the last ten UI-issued commands with per-call latency.
- **Outbox:** pending / processed counters, last processed timestamp, poll
  interval; polled every 2 s with a stale indicator on failure.
- **Recent events:** last 10 outbox rows with derived status
  (`pending` / `processed` / `failed`) and latency in milliseconds. Fed by
  `GET /actuator/outbox-recent`.
- **Read side:** author and book projections, auto-refreshed after each
  command with a visible consistency delay; partial, case-insensitive
  search by book title.
- **Write ↔ Read snapshot:** for the currently-tracked author or book,
  the Postgres row (normalised, with the `book_authors` join) next to
  the Mongo document (denormalised, with embedded author / book
  summaries) plus the last 10 event-log entries. Fed by
  `GET /actuator/entity-snapshot/{kind}/{id}`.

The primary observability surface of the demo — the only place where the
command → outbox → projection flow is visible in real time. Rationale in
[ADR 0009](docs/adr/0009-dashboard-as-first-class-observability-surface.md);
the LOC budget and the entity-snapshot contract are amended in
[ADR 0010](docs/adr/0010-dashboard-entity-snapshot-and-loc-cap-raise.md).

## Architecture

### Write flow (`demo`)

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

### Read flow (`demo`)

```
HTTP request ──► Controller ──► QueryBus ──► QueryHandler ──► MongoDB
```

The outbox solves the dual-write problem. The aggregate row and the event envelope are written in the same PostgreSQL transaction — either both commit or neither does. A scheduled poller drains the outbox, publishes each event through an in-memory bus, and Mongo projectors update the read model. No distributed transaction, no lost events, no ordering gap between "event published" and "state committed".

### Flow (`demo-vanilla`)

```
Main / Javalin ──► CommandBus ──► [ LoggingInterceptor ─► Handler ]
                                                             │
                                                             ▼
                                       InMemoryRepository.save()
                                                             │
                                                             ▼
                                     handler publishes events inline
                                                             │
                                                             ▼
                                     EventBus ──► Projection handlers
                                                             │
                                                             ▼
                                            InMemoryReadModel

QueryBus decorated with LoggingQueryBus (decorator, pending ADR 0014
for a symmetric QueryInterceptor pipeline in core).
```

No transactions (in-memory), so event publication happens inline in the command handler rather than via a `TransactionalCommandInterceptor`. Same `core`, different contract — exactly the point.

## Modules

- **`src/core`** — reusable CQRS framework. Published as a jar; Spring is an `<optional>` dependency, not transitive. **`core.contracts` and `core.ddd` have zero Spring imports — verified by ArchUnit.**
    - `core.contracts` — ports: `Command`, `CommandBus`, `CommandHandler`, `CommandInterceptor`, `Event`, `EventBus`, `EventHandler`, `Query`, `QueryBus`, `QueryHandler`.
    - `core.ddd` — `AggregateRoot<ID>`, `DomainEvent`.
    - `core.infrastructure.bus` — in-memory `SimpleCommandBus`, `SimpleQueryBus`, `SimpleEventBus`.
    - `core.infrastructure.spring` — the Spring adapter: `@EnableCqrs`, `CqrsConfiguration`, `BeanPostProcessor`s that auto-register handlers, `ValidationCommandInterceptor`, `TransactionalCommandInterceptor`.
- **`src/demo`** — Book bounded context, Spring Boot adapter. PostgreSQL + Flyway on the write side, MongoDB on the read side, outbox poller in between.
- **`src/demo-vanilla`** — Order bounded context, plain-Java adapter. Zero Spring. Manual bootstrap via `VanillaBootstrapper`, Javalin for HTTP, in-memory repository and read model. Logging via `LoggingCommandInterceptor` (using the core's own interceptor contract) and `LoggingQueryBus` (decorator, documented as temporary pending ADR 0014). Designed to be read as executable proof that `core` is portable. See [ADR 0013](docs/adr/0013-vanilla-adapter-demonstrating-framework-agnostic-core.md).

## Consuming `core` as a library

`core` is published to GitHub Packages as a versioned Maven artifact so it can be consumed from other repositories — the canonical way to prove the portability thesis at arm's length. Rationale and trade-offs in [ADR 0015](docs/adr/0015-publish-core-as-versioned-artifact.md).

Coordinates:

```xml
<dependency>
    <groupId>com.oscaruiz</groupId>
    <artifactId>mycqrs-core</artifactId>
    <version>1.3.1</version>
</dependency>
```

Declare the repository in the consumer's `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github-mycqrs</id>
        <url>https://maven.pkg.github.com/oscaruiz/myCQRS</url>
    </repository>
</repositories>
```

Authenticate in `~/.m2/settings.xml` with a GitHub PAT that has the `read:packages` scope:

```xml
<servers>
    <server>
        <id>github-mycqrs</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_PAT_WITH_read:packages</password>
    </server>
</servers>
```

> **GitHub Packages requires a PAT with `read:packages` even for public packages.** This is a GitHub limitation, not a project choice — expect to spend one minute creating a fine-scoped PAT the first time you consume the artifact.

Each released version is tagged `v<version>` in this repo; see [releases](https://github.com/oscaruiz/myCQRS/releases) for a list.

## Stack

- Java 21, Maven (multi-module).
- **`demo`:** Spring Boot 3.2.5, PostgreSQL + Flyway (`ddl-auto=validate`), MongoDB, Testcontainers.
- **`demo-vanilla`:** Javalin, Jackson, SLF4J simple. No Spring, no Micronaut, no Quarkus.
- JUnit 5, Mockito, AssertJ, ArchUnit.
- Docker (multi-stage build for `demo`), GitHub Actions CI.

## How to run it

Prerequisites: Java 21, Docker Desktop, and the bundled Maven wrapper (`mvnw.cmd`).

### `demo` (Spring Boot)

```powershell
# Start PostgreSQL and MongoDB in containers
docker compose -f src\demo\docker-compose.yml up -d postgres mongo

# Run the demo application (dev profile; Flyway applies V1/V2/V3 on boot)
.\mvnw.cmd spring-boot:run -pl src/demo
```

The API uses client-generated UUIDs: the client picks the identifier, `PUT` creates the resource at that URI. `Author` is a separate aggregate; a book with an author is three writes (create author, create book, link). See [ADR 0008](docs/adr/0008-authors-as-separate-write-operation.md).

```powershell
$BOOK   = "550e8400-e29b-41d4-a716-446655440000"
$AUTHOR = "b1e2c3d4-5678-90ab-cdef-1234567890ab"

# Create the author, then the book, then link them
curl.exe -X PUT "http://localhost:8080/authors/$AUTHOR" `
  -H "Content-Type: application/json" `
  -d '{\"firstName\":\"Sun\",\"lastName\":\"Tzu\",\"birthYear\":-544}'

curl.exe -X PUT "http://localhost:8080/books/$BOOK" `
  -H "Content-Type: application/json" `
  -d '{\"title\":\"The Art of War\"}'

curl.exe -X POST "http://localhost:8080/books/$BOOK/authors/$AUTHOR"

# Read (served from the Mongo projection, populated by the outbox poller)
curl.exe "http://localhost:8080/books/$BOOK"

# Update the title
curl.exe -X PATCH "http://localhost:8080/books/$BOOK" `
  -H "Content-Type: application/json" `
  -d '{\"title\":\"The Art of War (revised)\"}'

# Delete
curl.exe -X DELETE "http://localhost:8080/books/$BOOK"
```

`GET /books?title=…` looks a book up by title in the read model.

### `demo-vanilla` (plain Java)

No containers, no external services. Just the core, bootstrapped by hand:

```powershell
# Run the narrated flow — creates and confirms an Order, prints each step
.\mvnw.cmd -pl src/demo-vanilla exec:java

# Or, if you prefer HTTP (Javalin on :8080):
# The Main class starts a Javalin server in the same run.
```

The CQRS flow is visible in the console output — command in, event published, projection updated, query returning the new state. Intended to be read, not deployed. The same flow is exercised as an end-to-end test in `CqrsFlowDemonstrationTest`.

Framework independence is executable:

```powershell
.\mvnw.cmd -pl src/demo-vanilla dependency:tree | Select-String "springframework"
# (empty output)
```

## How to run the tests

```powershell
.\mvnw.cmd verify
```

Core tests run on JUnit + Mockito + AssertJ with no Spring context. `demo` integration tests boot `@SpringBootTest` against Testcontainers (PostgreSQL + MongoDB) wired via `@ServiceConnection`; H2 is not used anywhere. `demo-vanilla` tests run without Spring, without containers, against in-memory adapters — the assertion is that the `core` jar works in that environment too.

ArchUnit enforces package boundaries in CI: contracts and ddd must not depend on Spring, the Book context must follow an onion shape, command handlers must not call each other directly, and no module may contain a slice cycle.

## Design decisions

Significant decisions — including deliberate non-adoptions such as Event Sourcing — are documented as ADRs in [`docs/adr/`](docs/adr/). The README intentionally does not summarize them; open the directory when a specific choice matters.

## Status and roadmap

**Implemented**
- Framework-agnostic core with an explicit Spring adapter (`@EnableCqrs`, deferred import, `@ConditionalOnMissingBean` on every default bean).
- **Framework independence demonstrated by `demo-vanilla` — a second adapter consuming the same `core` jar with zero Spring dependencies. Proves by example what ArchUnit proves by assertion.**
- Handler auto-registration via `BeanPostProcessor`s for commands, queries, and events (`demo`); manual registration in `VanillaBootstrapper` (`demo-vanilla`).
- Chainable command interceptors in a fixed order: **validation → transaction → handler**. `TransactionalCommandInterceptor` uses `PlatformTransactionManager` with `PROPAGATION_REQUIRED` and commits outside the `try` block so a failing commit cannot trigger an invalid rollback.
- Outbox pattern (in `demo`): `OutboxEventBus` (marked `@Primary`) writes events to the outbox table inside the aggregate's transaction; `OutboxPoller` drains the table asynchronously and dispatches to the internal in-memory bus, where Mongo projectors subscribe.
- Optimistic locking on the aggregate via `@Version` on the JPA entity; `GlobalExceptionHandler` maps domain and infrastructure exceptions to meaningful HTTP status codes (e.g. `ObjectOptimisticLockingFailureException` → 409).
- Client-generated UUIDs; `PUT`/`PATCH`/`DELETE` for writes and `GET` for reads.
- Flyway migrations with `ddl-auto=validate` in every environment.
- ArchUnit enforcement of architectural boundaries in both modules.
- Testcontainers (PostgreSQL + MongoDB) for every `demo` integration test.
- Spring profiles for `dev` and `test` (`demo` only).
- Docker multi-stage image and GitHub Actions CI running `./mvnw verify`.
- Post-deploy smoke test against `/actuator/health` in the deploy workflow; each successful deploy registers a first-class GitHub Deployment against the `production` environment.

**Planned**
- Symmetric `QueryInterceptor` pipeline in core (ADR 0014) — currently `demo-vanilla` uses a decorator as a tactical workaround for query-side cross-cutting concerns.
- Idempotency: command deduplication on the write side and projection idempotency on the read side.
- End-to-end correlation ID propagated from HTTP request through command, event, and projection.
- A third adapter (Micronaut or Quarkus) to reinforce the portability thesis beyond two data points.

## License

GPL-3.0. See [LICENSE](LICENSE).

## Post-merge setup

One-time manual steps after the first merge to `main` triggers the deploy workflow:

- Provision a Neon Postgres free-tier database. Capture the JDBC URL (with `sslmode=require`), username, and password.
- Provision a MongoDB Atlas M0 cluster. Allow network access from `0.0.0.0/0` (Render free tier has no static IPs — documented trade-off). Capture the `mongodb+srv` URI including the database name.
- Create a Render Web Service in "Deploy an existing image from a registry" mode, pointing at `ghcr.io/<owner>/mycqrs:latest`. Set the four env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATA_MONGODB_URI`.
- Copy the Render deploy hook URL into the GitHub repository secret `RENDER_DEPLOY_HOOK`.
- Create GitHub repo secret `RENDER_APP_URL` containing the public URL of the Render service (no trailing slash). Used by the deploy workflow's post-deploy smoke test.
- In the GitHub repository settings, ensure workflow permissions are set to "Read and write" so the deploy workflow can push to GHCR.
- After the first successful deploy workflow run, flip the GHCR package visibility to public (GitHub profile → Packages → `mycqrs` → Package settings → Change visibility). Only needed once.
