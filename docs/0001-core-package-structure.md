# 0001 — Rename `core.domain` to `core.contracts` and defer further splits

**Status:** Accepted
**Date:** 2026-04-14

## Context

The `core` module is a CQRS framework consumed by bounded contexts (currently only `demo`). Its previous package structure had three problems:

- `core.domain.*` contained framework contracts (`Command`, `CommandBus`, `CommandHandler`, `Query`, `QueryBus`, `Event`, `EventBus`, etc.), not business domain. The name lied about the content.
- `core.domain` and `core.ddd` coexisted with similar names but distinct roles: the former held CQRS contracts, the latter DDD building blocks (`AggregateRoot`, `DomainEvent`). The similarity created confusion for any new reader.
- `core.infrastructure.spring.*` conflated two different roles: portable default implementations (`ValidationCommandInterceptor`, which depends only on `jakarta.validation`) with Spring-specific integration (`BeanPostProcessor`s, `@Configuration` classes, `TransactionalCommandInterceptor`).

The framework positions itself as lightweight and reusable. Its package names should reflect that intent honestly.

## Decision

This PR applies the following changes:

- Rename `core.domain.*` → `core.contracts.*`. The package now reflects its actual role: public contracts of the CQRS framework.
- Keep `core.infrastructure.*` and `core.ddd.*` structurally unchanged.
- Introduce `scripts/check-package-deps.sh` enforcing three static rules:
    - `contracts` must not import from `infrastructure` or `spring`.
    - `infrastructure` must not import from `spring`.
    - `ddd` must not import from `infrastructure` or `spring` (but may import from `contracts`, due to `DomainEvent implements Event`).

### Alternatives considered and rejected

- **`core.api` instead of `core.contracts`.** Rejected due to semantic collision. `demo/infrastructure/api/` already uses the word "api" in its colloquial sense (HTTP adapter). Reusing the name in `core/` with its technical sense (framework contracts) would have created ambiguity within the same repo. `contracts` is less conventional in the Java ecosystem (`*-api` is the pattern used by SLF4J, Jakarta EE, etc.) but resolves the collision and is self-explanatory.

- **Lift `core.spring` to top level (sibling of `core.infrastructure`).** Considered and deferred. The argument was to make the separation between portable default implementations and container-specific integration explicit — precedents: Spring Framework (`spring-core`, `spring-context`, `spring-tx`), SLF4J (`slf4j-api`, `slf4j-simple`, `logback-classic`). Rejected *for now* because only one consumer exists (Spring); extracting the abstraction with a single case usually yields the wrong abstraction. To be revisited when a second container is added (e.g. `demo-micronaut`).

- **Collapse the intermediate `core.infrastructure.bus/` package.** Considered and deferred under YAGNI. The collapse provides marginal clarity and blocks no pending work.

- **Keep `core.domain` as-is.** Rejected. The name lied about the content and created conceptual friction in every conversation about the code.

## Consequences

### Positive

- Package names reflect their actual role; the code self-documents better.
- The three architectural boundaries are enforced automatically by script.
- Pending architectural debt (splitting `spring`, collapsing `bus`) is explicitly documented with concrete revision criteria, not as a vague TODO.
- `contracts` is consumable without dragging in Spring or any implementation — an invariant verifiable by the dependency-check script.

### Negative / pending

- `scripts/check-package-deps.sh` is a low-tech substitute for ArchUnit. Migrate to ArchUnit when project size justifies it, or when more complex rules are needed (e.g. verifying that no `CommandHandler` depends on a concrete bus implementation).
- The decision about the location of `core.spring` remains open. It will be closed when a second container adapter exists.
- `core.contracts` still permits `jakarta.validation` as a transitive dependency via `ValidationCommandInterceptor`. This is deliberate (JSR specs are portable), but warrants vigilance: if `core.contracts` is later reconsidered as a fully spec-free API, `ValidationCommandInterceptor` must be moved entirely into `infrastructure`.
