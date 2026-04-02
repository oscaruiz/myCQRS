# myCQRS

![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)

## Purpose

This repository explores CQRS and DDD concepts by implementing core infrastructure manually instead of delegating all behavior to framework conventions. The intent is educational: make command, query, and event orchestration explicit in code.

## Architectural overview

The codebase is split between a small CQRS core and a demo book context.

In `core`, command, query, and event contracts are implemented with in-memory buses. Handler registration is automated via Spring `BeanPostProcessor` components, and command validation is applied through an interceptor.

In `demo`, domain behavior is centered on `BookAggregate`, which records domain events (`BookCreatedEvent`, `BookUpdatedEvent`, `BookDeletedEvent`) when state changes occur. Application handlers coordinate use cases through domain ports, and infrastructure adapters expose HTTP endpoints, persist write-side state with JPA, and update read/audit projections.

The layering follows a hexagonal style:

- Domain: aggregate, events, repository port.
- Application: command/query/event handlers.
- Infrastructure: API and persistence/projection adapters.

## Module structure

This is a multi-module Maven project:

- `src/core`: reusable CQRS abstractions and in-memory bus infrastructure.
- `src/demo`: runnable sample application built on top of `core`.

## Command execution flow

The write path in the demo is:

1. REST endpoint receives a command request.
2. Request is dispatched through `CommandBus`.
3. Command handler creates/loads `BookAggregate` and executes domain behavior.
4. Aggregate records domain events.
5. Aggregate state is persisted.
6. Emitted events are published through `EventBus`.
7. Event handlers update read projections and audit storage.

Query handling is separate and goes through `QueryBus` and query handlers.

## Running the application

The project uses Spring Profiles to separate configuration by environment.

**Dev (local):**

```bash
mvn spring-boot:run -pl src/demo
```

No flags needed — `application.yml` defaults to `profiles.active: dev`, which connects to local PostgreSQL and MongoDB.

**Test:**

```bash
mvn test
```

Integration tests automatically use the `test` profile via `@ActiveProfiles("test")`. This profile uses H2 in-memory and excludes MongoDB.

**Prod:**

```bash
SPRING_PROFILES_ACTIVE=prod java -jar mycqrs.jar
```

Or with a JVM flag:

```bash
java -Dspring.profiles.active=prod -jar mycqrs.jar
```

## Current limitations

Current known limitations are:

- Event dispatch is synchronous and in-memory.
- There is no outbox/transactional handoff for durable event publication.
- Delete projection coverage is partial.
- Some components still contain TODO or placeholder logic.
- Infrastructure logging still uses direct standard output in parts of the core.

## Testing approach

Tests focus on behavior across layers:

- Integration tests with Spring Boot and H2 for command-side flows.
- A smoke integration test for command-to-query behavior.
- Domain tests for aggregate invariants and domain event recording.
- Query handler tests for read-side lookup behavior.

## Future improvements

Planned improvements include:

- Asynchronous event dispatch.
- Durable publication strategy (for example, outbox pattern).
- Full delete projection support for read and audit models.
- Cleanup of placeholder handlers and pending TODO items.
- Logging and test layout cleanup.

## Architecture Overview

###  Hexagonal + CQRS + DDD Flow

      HEXAGONAL MAP + CQRS + DDD

                                  ┌──────────────────────────────┐
                                  │           CLIENT             │
                                  │     (HTTP / REST Call)       │
                                  └──────────────┬───────────────┘
                                                 │
                                                 ▼
                              ┌───────────────────────────────────┐
                              │           CONTROLLER              │
                              │  (Infra - HTTP Adapter)           │
                              │  - Parse @PathVariable Long id    │
                              │  - Build Command                  │
                              └──────────────┬────────────────────┘
                                             │
                                             ▼
                              ┌───────────────────────────────────┐
                              │           COMMAND BUS             │
                              │      (Core - Infra wiring)        │
                              └──────────────┬────────────────────┘
                                             │
                                             ▼
                        ┌────────────────────────────────────────────┐
                        │           COMMAND HANDLER (App)            │
                        │  - load aggregate via BookRepository       │
                        │  - call domain behavior                    │
                        │  - save aggregate                          │
                        │  - publish pulled domain events            │
                        └──────────────┬─────────────────────────────┘
                                       │
                                       ▼
                        ┌────────────────────────────────────────────┐
                        │              DOMAIN                        │
                        │        BookAggregate                       │
                        │                                            │
                        │  State: id, title, author, deleted        │
                        │  Methods: create/update/delete             │
                        │  Emits: Domain Events                      │
                        └──────────────┬─────────────────────────────┘
                                       │
                                       ▼
                        ┌────────────────────────────────────────────┐
                        │        BOOK REPOSITORY (PORT)              │
                        │        (Domain interface)                  │
                        └──────────────┬─────────────────────────────┘
                                       │
                                       ▼
                        ┌────────────────────────────────────────────┐
                        │      JPA BOOK REPOSITORY (ADAPTER)         │
                        │  - Maps Aggregate ↔ Entity                 │
                        │  - Implements load/save                    │
                        └──────────────┬─────────────────────────────┘
                                       │
                                       ▼
                        ┌────────────────────────────────────────────┐
                        │             BOOK ENTITY (JPA)              │
                        │   id | title | author | deleted            │
                        └──────────────┬─────────────────────────────┘
                                       │
                                       ▼
                                  ┌───────────────┐
                                  │  POSTGRES DB  │
                                  └───────────────┘

------------------------------------------------------------------------

###  Event Flow (CQRS Side)

    Aggregate.recordEvent(...)
              │
              ▼
    Handler pulls domainEvents
              │
              ▼
    EventBus.publish(event)
              │
              ▼
    Projection / Read Model
              │
              ▼
    Mongo / InMemory / Query Model

------------------------------------------------------------------------


## License

This project is licensed under the **GNU General Public License v3.0**.

See the [LICENSE](LICENSE) file for full details.
