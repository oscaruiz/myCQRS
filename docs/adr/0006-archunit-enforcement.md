# 6. ArchUnit enforcement of architectural boundaries

Date: 2026-04-20

## Status

Accepted

## Context

Architectural boundaries in this project (hexagonal inside the Book
bounded context; zero Spring inside core.contracts and core.ddd)
have been maintained so far by manual discipline. Manual discipline
degrades over time, with new collaborators, and with reviewer fatigue.
These boundaries are invariants of the design, not conventions —
and invariants that are not enforced do not exist.

The concrete decisions at stake:
- core.contracts and core.ddd must stay framework-agnostic (no
  dependency on org.springframework.*). This keeps the core module
  portable to a second DI framework (Micronaut, Quarkus) or to no
  framework at all (demo-vanilla, deferred post-v1).
- The Book bounded context follows hexagonal architecture: the
  domain depends on nothing, the application layer depends on the
  domain and on core contracts, and infrastructure adapters
  (jpa, mongo, api, outbox) sit on the outside.
- Command handlers must not depend on other command handlers
  directly. Coordination between commands must go through the
  CommandBus.
- No cyclic dependencies between slices of either module.

## Decision

Encode these invariants as ArchUnit tests executed in CI. A rule
violation fails the build.

Rules live in the module whose invariants they protect:
- Rules about core live in core/src/test.
- Rules about the Book bounded context live in demo/src/test.

Guiding principle: a test imports what it guards, never the other
way around. If the demo module is ever extracted, core keeps its
architecture tests. If a second bounded context is added to demo,
its rules live alongside it without touching Book's rules.

### Rules enforced

Rules are referenced by their `@ArchTest` method name below. Method
names are stable documentation anchors: numeric rule positions drift
when rules are added or reordered, but a name describing the
invariant survives the refactor.

core module (3 rules in `com.oscaruiz.mycqrs.core.ArchitectureTest`):

- `contractsDoNotDependOnSpring` — no class in ..core.contracts..
  depends on org.springframework..
- `dddDoesNotDependOnSpring` — no class in ..core.ddd.. depends on
  org.springframework..
- `coreSlicesAreFreeOfCycles` — no cyclic dependencies between
  slices of ..core.(*)..

demo module, Book bounded context (4 rules in
`com.oscaruiz.mycqrs.demo.book.ArchitectureTest`):

- `bookFollowsOnionArchitecture` — onion architecture: domain →
  application → adapters (jpa, mongo, api, outbox).
  `withOptionalLayers(true)` is applied to tolerate the implicit
  'domain service' layer being empty — domain logic lives in the
  aggregate.
- `bookApplicationDoesNotDependOnInfrastructure` — no class in
  ..demo.book.application.. depends on any class in
  ..demo.book.infrastructure.{jpa,mongo,api,outbox}.. This is
  redundant with `bookFollowsOnionArchitecture` on purpose: a
  failure here points at a broken boundary with a direct, readable
  message, without parsing the onion rule's output.
- `commandHandlersDoNotDependOnOtherCommandHandlers` — no command
  handler depends on another command handler implementation
  (excluding self-reference and the CommandHandler interface
  itself). See "Specific note" below.
- `bookSlicesAreFreeOfCycles` — no cyclic dependencies between
  slices of ..demo.book.(*)..

## Consequences

Positive:
- The build fails when a boundary is broken. Feedback is immediate
  and automatic.
- The tests are living documentation of the architectural decisions
  taken during Weeks 1–2: onion, framework-agnostic core, CQRS
  coordination via the bus.
- Boundary discussions stop being a recurring topic in code review.
  If a reviewer wants to argue about a boundary, the argument is
  about the rule, not about the code.

Negative:
- Maintenance cost when packages are renamed. Every rule that
  references a package path must be updated in lockstep with the
  refactor.
- Risk of false positives when a rule is over-specified. The
  handler→handler rule hit this on its first run: the initial
  predicate (assignableTo(CommandHandler) AND NOT equalTo(source))
  also captured the CommandHandler interface itself, because every
  handler class depends on the interface it implements and the
  interface is trivially assignable to itself. Detected the first
  time `mvn verify` ran against the new rule; reformulated by
  adding an explicit exclusion of the interface.

## Specific note — handler → handler rule

The rule forbids a handler from directly depending on another
handler (field injection, constructor parameter, method parameter,
etc.). Coordination between commands must go through the
CommandBus. Bus dispatch is a runtime lookup, not a class-level
dependency, and is the correct mechanism for orchestrating commands.

The predicate excludes three things intentionally:
1. Self-reference — a handler trivially references its own class
   through method signatures and fields.
2. The CommandHandler interface itself — every handler depends on
   the contract it implements; that is not "another handler".
3. Anything not assignable to CommandHandler — irrelevant.

What remains: concrete other handler implementations. Those are
forbidden as direct dependencies.

Iteration note: writing ArchUnit rules is iterative. The first
version of a rule almost always captures more than intended. The
discipline is to distinguish "the rule is too strict" (fix the
rule) from "the code violates the rule" (fix the code). This
reformulation was the first case; the second has not occurred yet.

## Alternatives considered

- Code review only. Depends on human discipline, does not scale
  beyond a small trusted team, and provides no feedback in CI.
  Rejected because the whole point of encoding invariants is to
  remove them from the reviewer's mental load.
- Separating each layer into its own Maven module. Captures some
  boundaries (module A cannot depend on module B without declaring
  it) but does not capture intra-module boundaries (application
  versus infrastructure inside demo). Disproportionate ceremony for
  a single bounded context. May be reconsidered if the project
  grows to multiple bounded contexts with shared kernel concerns.
- JPMS (Java Platform Module System). Heavy ceremony, tooling
  friction, and a steep learning curve for contributors, for a
  benefit that ArchUnit delivers with a single test dependency.
- jMolecules annotations. Adds a vocabulary (@AggregateRoot,
  @DomainService, ...) that the project does not currently need.
  Deferred; may be reconsidered if multiple bounded contexts appear
  and cross-context rules become worth expressing in a richer
  domain-level DSL.

## Scalability to future bounded contexts

Out of scope for this ADR. The anticipated pattern is one
ArchitectureTest per bounded context, with the onion rule
duplicated per context. Factoring the onion shape into a shared
helper is a possible evolution, but premature today — there is
only one bounded context. The decision between "duplicate per
context" and "factor into helper" is deferred to the moment a
second bounded context is introduced, at which point the
trade-off (readability of per-context tests vs. single source of
truth for the shape) will be concrete.
