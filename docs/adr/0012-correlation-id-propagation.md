# 12. Correlation ID propagation across the CQRS pipeline

Date: 2026-04-23

## Status

Accepted

## Context

A single logical request in this system spans at least five thread
boundaries: the Tomcat request thread (HTTP filter → controller →
command bus), the transaction boundary (commit triggers the outbox
INSERT), and the outbox poller thread (which, seconds later,
deserializes the event and dispatches it to projectors). Logs from
those threads today share no common key. Grepping by aggregate id
covers some scenarios but fails whenever:

- a command fails before an aggregate id is established (validation,
  command-parsing, filter errors),
- a single request mutates more than one aggregate,
- a projector failure in the poller cannot be tied back to the HTTP
  call that originally caused it, because the aggregate id exists in
  the event but the operator does not yet know which event they are
  hunting.

The operator needs a single key that ties every log line — request
thread, commit, poller thread, projector — to the originating HTTP
request, so that `grep '[<id>]'` yields a complete trace.

## Decision

Introduce a `correlationId` that lives in three places:

1. **HTTP boundary** — a servlet filter
   (`CorrelationIdFilter` in `demo`) reads `X-Correlation-ID` from
   the request, generates a fresh UUID if the header is absent or
   blank, places the value in SLF4J `MDC` under the key
   `correlationId`, echoes it back in the response header, and
   removes it from MDC when the chain returns.

2. **Synchronous command path** — a command-bus interceptor
   (`CorrelationIdCommandInterceptor` in `core`) registered as the
   outermost wrapper. If the MDC key is already set (filter
   populated it, or the outbox poller restored it before
   re-dispatching an event), the interceptor propagates the value
   and does *not* clear it on exit. If the key is absent, the
   interceptor generates a UUID, puts it in MDC, and removes it on
   exit (including when the chain throws). Ownership is explicit:
   the layer that puts the value is the layer that removes it.

3. **Async boundary (outbox)** — the `outbox` table carries a
   nullable `correlation_id UUID` column (migration V7). The
   outbox writer reads `MDC.get("correlationId")` at INSERT time
   and persists it on the row. The outbox poller reads the column
   per row, sets MDC before invoking projectors, and removes MDC
   in `finally`. Per-row, not per-batch — projectors across rows
   must not inherit the previous row's id.

The MDC key `"correlationId"` is hoisted to a shared constant
`core.infrastructure.observability.CorrelationIdMdc.KEY`, referenced
by every Java call site. A typo in any single caller would silently
break tracing without producing a compile error or a test failure,
so the hoist is load-bearing.

The interceptor lives under `core.infrastructure.observability`
(not `core.infrastructure.spring`): its only non-JDK import is
`org.slf4j.MDC`, and placing it under `.spring.` would make the
package name misrepresent its contents. An ArchUnit rule
`observabilityDoesNotDependOnSpring` forbids Spring imports from
this package.

The logback pattern in `demo/src/main/resources/logback-spring.xml`
inserts `[%X{correlationId:-}]` between the thread name and the
logger name. The `:-` default renders an empty string (not `null`)
when MDC is absent — log lines from outside a correlation scope
show empty brackets, not noise.

## Alternatives considered and rejected

- **Field on `Command`**. Every command record would have to
  declare and pass `correlationId`, every existing command in the
  demo module would need a migration, and every handler would
  become aware of a transport concern that has nothing to do with
  its domain. Rejected: the command contract should describe the
  business intent, not the tracing machinery.

- **Field in event payload JSON / on `DomainEvent`**. Same
  objection applied to events: domain events describe what
  happened in the business, not which HTTP request happened to
  trigger it. Additionally, backfilling existing events and
  keeping producers/consumers in sync on the schema is expensive
  for a tracing-only field. Rejected.

- **Global `ThreadLocal` outside MDC**. Reinvents what SLF4J
  already provides, loses the logback integration (the
  `[%X{...}]` layout converter), and requires teaching every
  logger in the system about the new thread-local. Rejected.

- **Rely on `aggregate_id` for tracing**. Covers per-aggregate
  flows only. Fails for cross-aggregate commands, for failures
  that happen before an aggregate is known (validation,
  deserialization, handler lookup), and for operator queries that
  start from "the HTTP request at 12:04:17" rather than "the book
  with id X". Rejected as insufficient.

- **`@Version`-style optimistic check that only logs**. Does not
  address tracing; addresses conflict detection, which is a
  separate concern already solved in ADR 0004.

- **Adopt OpenTelemetry / Micrometer tracing now**. Would deliver
  the correlation-id benefit and more (propagation over HTTP
  calls, spans, automatic instrumentation). Rejected for now
  because the blast radius (adding OTEL dependencies, agents,
  collector infrastructure) is disproportionate to the immediate
  need and locks the project into a vendor ecosystem earlier than
  the architectural exploration warrants. MDC + header is the
  minimum viable mechanism and is compatible with a future OTEL
  adoption (the W3C traceparent header can be mapped into the
  same MDC key, or MDC can be populated from OTEL spans).

## Consequences

Positive:

- A single `correlationId` key ties HTTP, command, outbox, and
  projector logs together. The operator can grep one string and
  reconstruct the full trace, including the async dispatch that
  happens on a different thread seconds later.
- The mechanism is explicit: three boundaries, three adapters,
  one constant. No framework magic. The code paths that set and
  clear MDC are directly readable.
- Core stays Spring-free: the interceptor lives in a package
  whose name (`observability`) describes its concern and whose
  ArchUnit rule enforces the absence of Spring imports.
- Open to evolution: a future OTEL adoption, an ETag-based
  conditional-request mechanism on top of the header, or a
  per-row retry-count correlation can reuse the same plumbing.

Negative:

- The outbox table gains a nullable column and an index. Rows
  written before V7, and rows written from a path that does not
  populate MDC (e.g. a scheduled job that creates commands
  without HTTP context), store `NULL`. The writer logs a WARN in
  that case; the row is still persisted.
- Tests that touch MDC must clear it in `@AfterEach` to prevent
  bleed between tests. This is a discipline the test authors
  must keep; a failure shows up as an unrelated test seeing a
  value set by an earlier test.
- The logback pattern change is global: every log line in the
  demo module gets the `[...]` slot. Consumers of the logs
  (dashboards, operators, scripts) must tolerate the new field.
  Mitigated by using `%X{correlationId:-}` so the field is empty
  rather than `null` when absent.
- Core declares `logback-classic` at **test scope only** to verify
  MDC behavior end-to-end within core test suite. Production
  consumers remain free to provide any SLF4J backend; the published
  `core` artifact carries no runtime logging implementation
  dependency. The framework-agnostic tesis is preserved at runtime;
  test-time we accept a concrete backend to avoid testing against a
  no-op MDC.

## Related work

- ADR 0003 — outbox pattern. The atomic `INSERT INTO outbox ...`
  committed with the aggregate UPDATE is what lets the
  correlation-id survive the async hop.
- ADR 0004 — optimistic locking. A 409 from a version conflict
  will be logged with the correlation id of the losing request,
  making it diagnosable.
- ADR 0006 — ArchUnit enforcement. The new
  `observabilityDoesNotDependOnSpring` rule is added in the same
  class as the existing `contractsDoNotDependOnSpring` /
  `dddDoesNotDependOnSpring` / `idempotencyDoesNotDependOnSpring`
  rules.
- ADR 0011 — command-level idempotency. The `correlationId`
  tracks a single invocation across threads; the idempotency key
  (`commandId`) dedupes retried invocations. They answer
  different questions ("which request?" vs "is this a replay?")
  and must not be conflated.
- Future ADR — OpenTelemetry adoption. When adopted, the MDC
  mechanism should be retired in favor of the W3C traceparent
  header, with a migration path that keeps `correlationId` as a
  baggage item until every consumer has migrated.
-  HTTP CRLF vectors are additionally rejected by the servlet container (Tomcat's RFC 7230 compliance). The filter's UUID validation remains the authoritative mitigation at the application layer and is covered by unit tests that bypass the container.
