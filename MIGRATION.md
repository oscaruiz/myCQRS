# Migration guide

## 1.3.1 → 1.4.0

**Summary**: Framework integrations (Spring, Micronaut) are now published as
separate Maven artifacts. `mycqrs-core` itself is fully framework-agnostic —
its POM no longer references Spring or any other container. Consumers pick
the integration they use and add it explicitly.

Rationale: [ADR 0016](docs/adr/0016-extract-framework-integrations-into-modules.md).

### What changed

In 1.3.1 the single `mycqrs-core` artifact declared Spring as four
`<optional>true</optional>` dependencies (`spring-context`,
`spring-boot-autoconfigure`, `spring-tx`, `spring-jdbc`). Consumers that
activated Spring received those transitively, even though the
framework-agnostic claim rested on that "optional" flag rather than on actual
packaging separation.

In 1.4.0 there are three artifacts, all sharing the same version and the
same git tag:

- `com.oscaruiz:mycqrs-core` — framework-agnostic. Zero container
  dependencies (only `jakarta.validation-api` and `slf4j-api`).
- `com.oscaruiz:mycqrs-spring` — Spring Boot integration (`@EnableCqrs`,
  `CqrsConfiguration`, the `BeanPostProcessor`s, Validation and Transactional
  command interceptors, and the Spring-JDBC adapter for idempotency).
- `com.oscaruiz:mycqrs-micronaut` — Micronaut integration (`@EnableCqrs`
  marker, `CqrsFactory`, `MicronautHandlerRegistrar`, Validation and
  Transactional command interceptors). **Idempotency is deliberately out of
  scope in this first release** — see ADR 0016.

### Java API: no changes

Package names and class names are unchanged.
`com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs`,
`CqrsConfiguration`, `TransactionalCommandInterceptor`,
`ValidationCommandInterceptor`, and the `BeanPostProcessor`s all live in the
same package, just in a different jar. **Every Java `import` that worked in
1.3.1 still works in 1.4.0.** No code changes are required in the consumer's
Java source.

### If you consume the Spring adapter

Add `mycqrs-spring` to your `pom.xml`.

**Before (1.3.1)**:

```xml
<dependency>
    <groupId>com.oscaruiz</groupId>
    <artifactId>mycqrs-core</artifactId>
    <version>1.3.1</version>
</dependency>
```

**After (1.4.0)**:

```xml
<dependency>
    <groupId>com.oscaruiz</groupId>
    <artifactId>mycqrs-core</artifactId>
    <version>1.4.0</version>
</dependency>
<dependency>
    <groupId>com.oscaruiz</groupId>
    <artifactId>mycqrs-spring</artifactId>
    <version>1.4.0</version>
</dependency>
```

The explicit dependency on `mycqrs-core` stays for clarity (it is transitive
via `mycqrs-spring`, but declaring it directly documents intent and survives
future refactors of the transitive graph).

### If you consume the Micronaut adapter (new in 1.4.0)

There is no "before": Micronaut support did not exist in 1.3.1.

```xml
<dependency>
    <groupId>com.oscaruiz</groupId>
    <artifactId>mycqrs-core</artifactId>
    <version>1.4.0</version>
</dependency>
<dependency>
    <groupId>com.oscaruiz</groupId>
    <artifactId>mycqrs-micronaut</artifactId>
    <version>1.4.0</version>
</dependency>
```

Activation in a Micronaut application is implicit — adding the module to the
classpath is sufficient (`CqrsFactory` and `MicronautHandlerRegistrar` are
discovered by Micronaut's compile-time annotation processor). The
`@EnableCqrs` marker annotation is provided for source-level symmetry with
the Spring adapter and has no runtime effect.

If your application requires command-level idempotency
([ADR 0011](docs/adr/0011-command-level-idempotency.md)), note that
`mycqrs-micronaut` does **not** wire `IdempotencyCommandInterceptor`. The
only `ProcessedCommandsStore` implementation today is
`JdbcProcessedCommandsStore` in `mycqrs-spring`, which is Spring-JDBC
specific. A future release will ship a Micronaut-Data JDBC equivalent.
Until then, consumers that need idempotency should use the Spring adapter
or wait for the follow-up release tracked in the README roadmap. There is
no public extension point today for a consumer to register their own store
in the Micronaut adapter (see ADR 0016).

### If you consume `mycqrs-core` without any framework

Nothing to change: `mycqrs-core` is still the right artifact, and it is now
free of any Spring transitive noise. See `src/demo-vanilla` for a runnable
example.

### Version coupling requirement

`mycqrs-core` and the integration module you choose (`mycqrs-spring` or
`mycqrs-micronaut`) must stay at the same version. Mixing versions — for
example `mycqrs-core:1.5.0` with `mycqrs-spring:1.4.0` — is **unsupported**
and may produce runtime `NoSuchMethodError` or `NoClassDefFoundError` when
the integration module invokes an internal API of the core whose shape
changed between releases.

The integration modules declare their required `mycqrs-core` version with
an exact match in their published POM (verified in the flattened
`mycqrs-spring-X.Y.Z.pom` and `mycqrs-micronaut-X.Y.Z.pom`). Downstream
consumers should keep the two coordinates in lockstep when upgrading.

### Versioning rationale

1.4.0 is a minor bump. Under a strict SemVer reading this is arguably a
major change because existing Spring consumers must edit their `pom.xml`.
The minor bump is accepted because the Java API is unchanged — every import,
every class, every method signature from 1.3.1 still works in 1.4.0 — and
because the only 1.3.1 consumer today is this repository itself. See ADR
0016, §"Versioning: `1.4.0` (minor)" for the full argument.

### GitHub Packages setup

The authentication and repository-declaration steps are unchanged from
1.3.1; see the README's "Consuming `core` as a library" section. The same
`settings.xml` that worked for `mycqrs-core:1.3.1` works for all three
artifacts at `1.4.0` — they share the same GitHub Packages repository.
