# 16. Extract framework integrations into sibling Maven modules

Date: 2026-04-24

## Status

Accepted. Completes a decision deferred in [ADR 0001](0001-core-package-structure.md).

## Context

[ADR 0001](0001-core-package-structure.md) explicitly deferred the question of
separating the Spring-specific integration from the framework core:

> **Lift `core.spring` to top level (sibling of `core.infrastructure`).** Considered
> and deferred. […] Rejected *for now* because only one consumer exists (Spring);
> extracting the abstraction with a single case usually yields the wrong abstraction.
> To be revisited when a second container is added (e.g. `demo-micronaut`).

Between then and today, the shape of `core` followed that plan:

- `com.oscaruiz.mycqrs.core.infrastructure.spring.*` lived inside the published
  `mycqrs-core` jar.
- `src/core/pom.xml` declared four Spring dependencies as `<optional>true</optional>`:
  `spring-context`, `spring-boot-autoconfigure`, `spring-tx`, `spring-jdbc`.
- [ADR 0013](0013-vanilla-adapter-demonstrating-framework-agnostic-core.md) made
  the "framework-agnostic" claim executable by consuming the same `mycqrs-core`
  jar from `demo-vanilla` without Spring.
- [ADR 0015](0015-publish-core-as-versioned-artifact.md) put `mycqrs-core` on
  GitHub Packages so external consumers could exist at arm's length.

The trigger that ADR 0001 named — *a second container* — is now in front of us.
Adding a Micronaut adapter makes the `<optional>` model collapse:

- `mycqrs-core` would have to declare `micronaut-context`, `micronaut-validation`,
  `micronaut-data-tx` also as `<optional>`, on top of the four Spring optionals.
- Every consumer would pull a POM advertising both ecosystems, even though most
  consumers care about exactly one.
- A third or fourth adapter (Quarkus, Helidon, plain `new`) multiplies the noise.

This is the same shape the Java ecosystem has already solved elsewhere: Axon
Framework, Spring Modulith, jMolecules, and Micronaut itself split the core from
integration adapters across Maven modules, not via `<optional>` flags.

## Decision

Split the core along the integration boundary into three published artifacts with
a single shared version:

- `com.oscaruiz:mycqrs-core` — framework-agnostic. Zero dependencies on any DI
  container. The only runtime deps are `jakarta.validation-api` and `slf4j-api`.
- `com.oscaruiz:mycqrs-spring` (new module `src/core-spring`) — the Spring Boot
  adapter. Consumes `mycqrs-core`; declares `spring-context`,
  `spring-boot-autoconfigure`, `spring-tx`, `spring-jdbc` as compile-scope (no
  longer `<optional>`).
- `com.oscaruiz:mycqrs-micronaut` (new module `src/core-micronaut`) — the
  Micronaut adapter. Consumes `mycqrs-core`; declares `micronaut-context`,
  `micronaut-inject`, `micronaut-validation`, `micronaut-data-tx` as compile
  scope.

Operational rules carried over from ADR 0015:

- **Single version axis.** The reactor, all three published artifacts, and both
  demos share the same version. One `v<version>` tag labels the repo as a whole.
- **Tag-triggered publishing.** The same workflow
  (`.github/workflows/publish-core.yml`) that used to publish only `mycqrs-core`
  now publishes all three in a single step:
  `./mvnw -pl src/core,src/core-spring,src/core-micronaut deploy` (**no `-am`** —
  the three target modules declare each other as dependencies, and the reactor
  builds them in the right order on its own).
- **Flatten the published POM.** All three modules flatten their POM with
  `flatten-maven-plugin` in `oss` mode so consumers never need access to the
  reactor parent.

Java package names are **unchanged**:
`com.oscaruiz.mycqrs.core.infrastructure.spring.*` remains the Spring adapter's
namespace, `com.oscaruiz.mycqrs.core.infrastructure.micronaut.*` the Micronaut
adapter's. What changes is which jar provides those classes. Every Java import
that worked in 1.3.1 still works in 1.4.0.

### Micronaut adapter: parity-minus-idempotency

The first cut of `mycqrs-micronaut` wires **three** of the four command
interceptors that the Spring adapter wires: `CorrelationId → Validation →
Transactional → Handler`. It does **not** wire `IdempotencyCommandInterceptor`
([ADR 0011](0011-command-level-idempotency.md)). The Spring adapter backs the
idempotency store with `JdbcProcessedCommandsStore`, a `spring-jdbc` adapter on
top of `NamedParameterJdbcTemplate`. A Micronaut-Data equivalent would be a
second implementation of the `ProcessedCommandsStore` port, which is a separate
scope of work with its own testing surface and its own dependency on
`micronaut-data-jdbc`. Deferring it here keeps the first Micronaut adapter small
enough to reason about end-to-end.

Consequence for consumers: a Micronaut application that requires at-least-once
deduplication of commands has two first-class options — (a) use the Spring
adapter instead, or (b) wait for a future release that ships a Micronaut-Data
JDBC implementation of `ProcessedCommandsStore` alongside the necessary
`CqrsFactory` wiring. There is **no public extension point today** for a
consumer to register their own `ProcessedCommandsStore` in the Micronaut
adapter: `CqrsFactory.commandBus(...)` builds the bus internally without
exposing an interceptor hook, so "DIY idempotency" would require overriding
the whole `CommandBus` bean and reimplementing the factory's interceptor
chain. That is possible but out of scope for this release; the follow-up in
the README roadmap tracks the proper wiring. Applications that do not need
idempotency are unaffected.

### Versioning: `1.4.0` (minor)

`1.4.0` is a minor bump. A strict reading of SemVer would argue this is a major
change because the consumer's `pom.xml` must be edited: Spring consumers who
used to depend on `mycqrs-core` alone now also need `mycqrs-spring`. The minor
bump is accepted on two grounds:

- **The Java API is unchanged.** Every import, class name, method signature,
  and annotation in 1.3.1 still works in 1.4.0. No code compiled against 1.3.1
  needs a line changed in Java to keep compiling. The breakage is in Maven
  packaging, not in the library's contract to its callers.
- **There are no external consumers today.** The only 1.3.1 consumer is this
  repo itself. A major bump would inflate the perceived scope of the change
  without protecting anyone.

The migration step is documented in `MIGRATION.md` at the repo root, and the
README's "Consuming `core` as a library" section now shows three variants.

## Alternatives considered

- **Keep `<optional>` with both frameworks' dependencies.** Rejected. Adding
  Micronaut deps as `<optional>` alongside the Spring ones grows the `mycqrs-core`
  POM from four framework optionals to eight, on a path that keeps widening with
  every future adapter. A consumer reading the POM would have to cross-reference
  the javadoc to know which subset is "theirs". The `<optional>` model was
  justifiable with one container (ADR 0001); it is not justifiable with two.

- **Adapters in the consumer's repository, not in this one.** Considered. Each
  consumer would bring its own adapter implementation. Rejected: the adapter is
  *reusable code* — a second Micronaut consumer should not have to re-derive the
  handler auto-registrar, the interceptor order, or the validation bridge. A
  library that forces every downstream project to re-implement the same plumbing
  is not a library. Keeping the adapters here and publishing them makes the
  reuse effective.

- **Version `2.0.0` (strict SemVer).** Considered. A strict reading treats any
  change that forces a `pom.xml` edit on the consumer as breaking, regardless of
  the Java API. Rejected because a `2.0.0` on an unchanged API signals more
  severity than is warranted, and there are no external users whose expectation
  of semver needs protecting. The migration is documented in one paragraph in
  `MIGRATION.md`; if that proves insufficient, the next breaking change can
  correct the versioning posture.

- **Re-exporting `mycqrs-spring` transitively from `mycqrs-core` for backwards
  compatibility.** Rejected. This is over-engineering for zero benefit: it
  would defeat the entire point of keeping `mycqrs-core` framework-agnostic, and
  there are no external consumers whose 1.3.1 POMs must keep working
  unmodified. The one affected POM in this repo (`src/demo/pom.xml`) is updated
  in the same PR.

- **Use `io.micronaut.platform:micronaut-platform` BOM in
  `core-micronaut/pom.xml`.** Attempted and rejected at implementation time.
  The platform meta-BOM at version *N* pins runtime `micronaut-core` /
  `micronaut-inject` to a version older than the matching annotation
  processors, which fails compile with
  `NoSuchMethodError: AbstractInitializableBeanDefinition#isMethodResolved`.
  Switched to `io.micronaut:micronaut-core-bom`, the narrower BOM that governs
  only the core artifacts, with validation / data-tx / test pinned alongside.
  This alignment is documented inline in the POM so the decision is visible at
  the point of change.

## Consequences

### Positive

- **`mycqrs-core` is genuinely framework-agnostic.** Its flattened, published
  POM contains zero references to `org.springframework.*` or `io.micronaut.*` —
  verified by the local smoke test in Paso 5 of the implementation plan.
  `demo-vanilla` (ADR 0013) no longer relies on the honour system of
  `<optional>` to claim portability; it now rests on an empty dependency graph.
- **The split is enforced, not just conventional.** Two new ArchUnit rules live
  alongside the code:
    - `core-spring` test: no class in `..core.infrastructure.spring..` may
      depend on `io.micronaut..`.
    - `core-micronaut` test: no class in `..core.infrastructure.micronaut..`
      may depend on `org.springframework..`.
  The pre-existing rules in `core` (`contractsDoNotDependOnSpring`,
  `dddDoesNotDependOnSpring`, `idempotencyDoesNotDependOnSpring`,
  `observabilityDoesNotDependOnSpring`) remain — they become trivially true
  for the current class set, but they continue to guard against regression if a
  future PR adds a Spring import to core.
- **The pattern scales.** A third adapter (Quarkus, Helidon, etc.) is a new
  `src/core-<framework>` module, a new line in the reactor's `<modules>`, a
  new entry in the `-pl` list of the publish workflow, and a new ArchUnit
  isolation rule. Nothing in `mycqrs-core` needs to change.

### Negative / accepted costs

- **Three artifacts per release instead of one.** More bytes on GitHub Packages,
  three entries to verify in the "Packages" tab after a tag push. Accepted —
  this is the shape of the ecosystem references (Axon, Spring Modulith,
  jMolecules).
- **Spring consumers must update their POM on upgrade.** Every existing
  consumer of 1.3.1 that uses the Spring adapter gets a compile-time failure
  when they bump to 1.4.0, because the `core.infrastructure.spring.*` classes
  are no longer on the classpath. `MIGRATION.md` documents the one-line fix
  (add `mycqrs-spring` dependency). This is the one-time cost of correcting the
  packaging.
- **Micronaut adapter is partial.** See "Micronaut adapter: parity-minus-
  idempotency" above. Documented; not a regression (there was no Micronaut
  adapter in 1.3.1).

## Related work

- **ADR 0001** — this ADR completes the deferred decision in §"Lift
  `core.spring` to top level". No supersession: 0001's reasoning ("one container
  is not enough to justify the abstraction") is accepted as-is. The condition
  it named has been met.
- **ADR 0013** — the vanilla adapter. 0016 reinforces (does not supersede) that
  ADR's portability thesis: it escalates the proof from "`<optional>` means the
  dep is not transitive" to "there is no dep at all in the core POM".
- **ADR 0015** — publishing infrastructure. 0016 extends the same tag-triggered
  workflow from one artifact to three, keeping the operational model identical.
- **ADR 0011** — command-level idempotency. 0016 does not touch the idempotency
  mechanism itself; it notes that the Micronaut adapter does not wire the
  interceptor for this first cut, and that the `JdbcProcessedCommandsStore` is
  a Spring-JDBC adapter that moves to `mycqrs-spring` intact.
- **ADR 0014 (pending)** — symmetric QueryInterceptor pipeline. Unrelated to
  this split.

## Notes for future revision

- If a Micronaut consumer needs idempotency, implement
  `MicronautJdbcProcessedCommandsStore` (using Micronaut Data's JDBC runtime)
  and add it to `core-micronaut`'s `CqrsFactory` as an optional bean (analogous
  to the current Spring adapter wiring).
- If a third adapter is added (Quarkus, Helidon), replicate the
  `core-<framework>` module layout and the ArchUnit isolation rule. No ADR
  amendment needed unless the split pattern itself changes.
- If external consumers of the Spring adapter complain about the mandatory POM
  edit, consider releasing a transitional `mycqrs-core-spring-compat` shim that
  re-exports `mycqrs-spring`. No consumer has asked; deferred.
