# AGENTS.md

## 1. Purpose of This Document

This document defines strict rules and architectural constraints for any
autonomous agent (e.g., Codex) modifying this repository.

The project is not a demo CRUD application. It is an architectural
laboratory designed to:

-   Explore CQRS and Domain-Driven Design (DDD) in depth.
-   Implement core infrastructure manually (without Axon or similar
    frameworks).
-   Experiment with architectural trade-offs (synchronous vs
    asynchronous dispatch, consistency guarantees, outbox pattern,
    etc.).
-   Serve as a long-term professional portfolio asset oriented toward
    backend/architecture roles.

Agents modifying this repository must preserve architectural intent over
convenience.

------------------------------------------------------------------------

## 2. Project Purpose

**myCQRS** is a custom CQRS infrastructure built in Java using Spring
Boot, structured as a multi-module Maven project.

The objective is educational but serious:

-   Understand command, query, and event mechanics by implementing them
    from scratch.
-   Model aggregates that emit domain events.
-   Separate write and read models.
-   Maintain an append-only audit log.
-   Gradually evolve toward stronger guarantees (outbox, optimistic
    locking, event replay).

This repository is intended to demonstrate architectural reasoning, not
just functional correctness.

------------------------------------------------------------------------

## 3. High-Level Architecture

### 3.1 Architectural Principles (Mandatory)

All changes must comply with:

-   **CQRS**

-   **Domain-Driven Design (DDD)**

-   **Hexagonal Architecture**

-   Clear separation of:

    -   Domain
    -   Application
    -   Infrastructure

The aggregate is the center of business logic.

The handler coordinates.  
The aggregate decides.  
The infrastructure adapts.

------------------------------------------------------------------------

### 3.2 Module Structure

Multi-module Maven:

    src/core
    src/demo

#### `core`

Reusable CQRS infrastructure:

-   `CommandBus`
-   `QueryBus`
-   `EventBus`
-   `CommandHandler`, `QueryHandler`, `EventHandler`
-   Interceptors
-   Spring auto-registration via `BeanPostProcessor`

No domain logic belongs here.

#### `demo`

Example bounded context (“Books”):

-   Domain:

    -   `BookAggregate`
    -   Domain events
    -   Repository port

-   Application:

    -   Command handlers
    -   Query handlers
    -   Event projections

-   Infrastructure:

    -   REST controllers
    -   JPA adapters (Postgres)
    -   Mongo projections
    -   Audit log

------------------------------------------------------------------------

## 4. Core Design Rules

### 4.1 Aggregate Rules (Strict)

-   Aggregate generates domain events internally.
-   Handlers must NOT instantiate domain events directly.
-   Aggregate maintains an internal list of domain events.
-   Events are retrieved via `pullDomainEvents()`.
-   Domain events represent facts, not intentions.

Do not introduce business logic into handlers.

------------------------------------------------------------------------

### 4.2 Command Handlers

Command handlers must:

1.  Load or create aggregate.
2.  Execute domain behavior.
3.  Persist aggregate.
4.  Publish pulled domain events.

Handlers must NOT:

-   Contain validation logic (use interceptors or domain).
-   Instantiate domain events manually.
-   Access infrastructure directly outside defined ports.

------------------------------------------------------------------------

### 4.3 Query Handlers

-   Must not modify state.
-   Must access read model only.
-   Must not access aggregates directly.

------------------------------------------------------------------------

### 4.4 Event Bus

Currently:

-   In-memory
-   Synchronous
-   Non-durable

Agents must NOT introduce distributed messaging (Kafka, etc.) without
explicit structural migration planning.

Future-proofing (allowed):

-   Outbox integration
-   Async dispatch via executor

------------------------------------------------------------------------

## 5. Persistence Rules

### 5.1 Write Model

-   PostgreSQL (via JPA)
-   Aggregate identity is domain-generated (UUID)
-   `@Version` must be used for optimistic locking once introduced
-   No business logic in JPA entities

### 5.2 Read Model

-   MongoDB
-   `_id` = aggregateId
-   Upsert semantics for create/update
-   Delete must remove read model document

### 5.3 Audit Log

-   Append-only
-   Never update existing records
-   Store serialized event payload

------------------------------------------------------------------------

## 6. Feature Development Rules

When adding a new feature:

1.  Start in Domain (aggregate behavior).
2.  Add domain event if state changes.
3.  Update command handler.
4.  Update read model projection.
5.  Update audit projection.
6.  Add tests (domain + integration).

Never start from controller downward.

------------------------------------------------------------------------

## 7. Naming Conventions

### Aggregates

`<Entity>Aggregate`

### Events

`<Entity><Action>Event`

Examples:

-   `BookCreatedEvent`
-   `BookUpdatedEvent`
-   `BookDeletedEvent`

### Commands

`<Action><Entity>Command`

### Queries

`Find<Entity>By<Criteria>Query`

### Handlers

`<CommandName>Handler`  
`<QueryName>Handler`

------------------------------------------------------------------------

## 8. Error Handling Rules

-   Domain-specific exceptions must extend a custom `DomainException`.
-   Application layer must not leak infrastructure exceptions.
-   REST layer must map domain exceptions via `@ControllerAdvice`.
-   Never return generic 500 for domain errors.

------------------------------------------------------------------------

## 9. Testing Rules

### 9.1 Required Levels

Every behavior change must include:

-   Domain unit test
-   Application-level test (if orchestration changes)
-   Integration smoke test (command → query path)

### 9.2 Constraints

-   H2 for write-side tests
-   Mongo disabled or mocked for integration unless specifically testing
    projections
-   No test should depend on incremental numeric IDs

### 9.3 Event Assertions

Tests should verify:

-   Correct number of emitted events
-   Correct event types
-   Correct read model state after events

------------------------------------------------------------------------

## 10. Logging Rules

-   Use SLF4J.
-   Never use `System.out`.
-   Do not log sensitive data.
-   Logging must not alter business flow.

------------------------------------------------------------------------

## 11. Dependencies

External dependencies are limited to:

-   Spring Boot
-   Spring Data JPA
-   Spring Data MongoDB
-   Jakarta Validation
-   H2 (tests)

Do NOT introduce:

-   Axon
-   Kafka
-   External CQRS frameworks
-   Hidden magic abstractions

All architectural mechanisms must remain explicit.

------------------------------------------------------------------------

## 12. Build and Execution

### Compile

    mvn clean install

### Run Demo

    mvn -pl src/demo spring-boot:run

Requires:

-   PostgreSQL running
-   MongoDB running

### Run Tests

    mvn -pl src/demo -am test

Keep this command green at all times.

------------------------------------------------------------------------

## 13. Commit Rules

Use structured commit messages:

    feat(domain): introduce optimistic locking in BookAggregate
    fix(core): prevent duplicate handler registration
    test(demo): add delete projection integration test
    refactor(core): replace System.out with SLF4J

Never commit:

-   Debug prints
-   Partially implemented TODOs
-   Broken tests

------------------------------------------------------------------------

## 14. What the Agent Must NOT Do

-   Do not move business logic to infrastructure.
-   Do not create shortcuts that violate CQRS separation.
-   Do not introduce implicit coupling between core and demo.
-   Do not convert this into CRUD disguised as CQRS.
-   Do not oversell features (e.g., claim full Event Sourcing if not
    implemented).
-   Do not introduce framework magic that hides architectural mechanics.

------------------------------------------------------------------------

## 15. Professional Objective Constraint

This repository serves as a long-term architectural learning asset and
portfolio piece.

Changes must:

-   Increase architectural clarity.
-   Improve reasoning quality.
-   Preserve hexagonal boundaries.
-   Demonstrate understanding of trade-offs.

Agents must prioritize correctness, clarity, and explicitness over
speed.

------------------------------------------------------------------------

End of AGENTS.md
