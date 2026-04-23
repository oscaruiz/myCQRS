## Dual enforcement: `scripts/check-package-deps.sh` vs ArchUnit

Two systems enforce architectural invariants in this project:

1. `scripts/check-package-deps.sh` — grep-based, runs before Maven in CI.
2. ArchUnit tests in `core` and `demo` — run inside `mvn verify`.

Their rule sets are different, not redundant. Open question: is this
intentional complementarity (shell as a fast pre-commit subset of
ArchUnit), real complementary coverage (the shell script enforces
rules ArchUnit does not), or historical residue (the script predates
ArchUnit and was never retired)?

Evidence it has not been audited recently: the second rule in the
shell script forbids imports from `core.infrastructure.*` to
`core.spring.*`, but `core.spring` does not exist as a top-level
package — Spring integration lives under `core.infrastructure.spring`.
That rule is a no-op.

Action deferred post-v1: pick one of the three
interpretations and act. If intentional subset, document the
contract. If complementary, migrate missing rules to ArchUnit and
retire the shell script. If residue, retire the shell script.

Leaving an enforcement system in place without auditing its rules
is worse than not having it — it provides false confidence.

## Interceptor package grouping

`ValidationCommandInterceptor` currently lives under
`core.infrastructure.spring` but does not import Spring (only
`jakarta.validation`). Colocation is historical, not principled —
`.spring.` is reserved for classes that actually import Spring
symbols (`TransactionalCommandInterceptor` does, via
`PlatformTransactionManager`; the bean post-processors do, via
`BeanPostProcessor`). `CorrelationIdCommandInterceptor` was placed
correctly under `core.infrastructure.observability` from the start;
`ValidationCommandInterceptor` is the remaining outlier.

Re-evaluate next time interceptor packages are touched. If moved,
the natural destination is `core.infrastructure.validation`, and
the ArchUnit rule shape `observabilityDoesNotDependOnSpring` should
be duplicated for the new package so the invariant is enforced.
