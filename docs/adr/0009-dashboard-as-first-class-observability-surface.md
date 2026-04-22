# 0009 — Dashboard is the primary observability surface for the CQRS pipeline

**Status:** Accepted
**Date:** 2026-04-22

## Context

PR 2 shipped a minimal HTML dashboard at `/` as an explicit "second-class
citizen": no framework, no tests, ~300 LOC, and a README disclaimer that
framed it as a convenience on top of the real surface (Swagger + Actuator).
The intent was to keep the architectural contract unambiguous — Swagger for
API reference, Actuator for introspection, dashboard as a demo helper that
could be ignored.

Local testing after PR 2 landed made it clear that framing was wrong. The
dashboard is not a shortcut to Swagger. It is the only place in the system
where the full CQRS flow — command issuance, outbox write, poller lag,
projection catch-up, read-side query — is visible end-to-end, in real time,
without a debugger or a second terminal. Swagger shows endpoint shapes.
Actuator returns JSON. Neither tells the observer *when the projection
catches up relative to the command that triggered it*. The dashboard does.

This also recasts what the dashboard is *for*. It is not a convenience
layer on top of the API — it is the functional prototype for an eventual
frontend module (likely React, not committed yet). Keeping it inside the
demo jar as a single static file lets the information architecture evolve
quickly without a build step. When the shape stabilises, that module ships
separately and supersedes this ADR.

PR 3 acts on the repositioning: the LOC ceiling moves from 350 to 500, a
new public observability endpoint (`/actuator/outbox-recent`) feeds a
recent-events panel, a command-history card shows the last ten UI-issued
calls with latency, and a search card exercises the read side by exact
title. The README stops calling the dashboard second-class.

## Decision

The dashboard is promoted to the primary observability surface of the
demo. It is the canonical way to watch the CQRS pipeline run during
demos, and it is the prototype for a future dedicated frontend module.

### Changes

- **LOC budget** on `src/demo/src/main/resources/static/index.html` is
  raised from 350 to 500. Exceeding 500 is the signal that the dashboard
  has outgrown a single-file prototype and should graduate to its own
  module with a build pipeline — not that the budget should be raised
  again.
- **No framework, no build step, no dependencies** inside the dashboard
  file. Plain HTML + CSS + vanilla JS served directly by Spring Boot's
  static handler. This is deliberate: it lets anyone read the whole
  surface in one file, keeps the demo jar self-contained, and removes
  friction when iterating on shape.
- **No tests on the dashboard** (the file itself). The prototype is
  reviewed visually; tests land when the dashboard moves into its own
  module.
- **New public observability endpoint:** `GET /actuator/outbox-recent`
  (PR 3). Returns the last N (default 10, max 50) outbox rows with
  `id`, `event_type`, `event_type_simple`, `aggregate_type`,
  `occurred_at`, `processed_at`, `latency_ms`, and `status`
  (`pending` / `processed` / `failed`). `aggregate_type` is derived from
  the event FQCN at query time — the schema is not touched.
- **README** is rewritten to drop "second-class citizen" framing, list
  what the dashboard actually shows, and point at this ADR. The section
  is moved up, closer to Live demo / Quick start, away from the bottom.

### What the dashboard shows end-to-end

- **Write side (Column 1):** author and book create/update/delete,
  author→book link, command latency, last N issued commands.
- **Pipeline middle (Column 2 + wide row):** outbox counters
  (pending / processed / last_processed_at / poll interval) plus the
  last 10 events with derived status and latency.
- **Read side (Column 3):** author projection, book projection (auto-
  refreshed after each command with a visible consistency delay, see the
  inline JS rationale), and a single-result title search.

## Alternatives considered and rejected

- **Keep the dashboard at 350 LOC, accept limited observability.**
  Rejected. The dashboard is already the entry point observers open
  during demos, whether the ADR frames it as "second-class" or not. A
  cap that is lower than what honest observability requires is a forcing
  function in the wrong direction: either the cap is ignored as soon as
  it is inconvenient, or the demo surface stays visibly incomplete. The
  right cap is one that fits the actual use case, with a clear exit
  condition (500 LOC) for when the surface has outgrown a single file.

- **Rewrite the dashboard in React now, inside its own module.**
  Rejected as premature. The information architecture is still moving:
  PR 3 alone adds three new UI surfaces (events panel, command history,
  search card) and adjusts the auto-refresh semantics. Locking a JS
  framework and a build pipeline around a shape that is actively being
  prototyped buys nothing and slows iteration. React (or whatever
  framework ships later) lands when the panels stop moving.

- **Expose observability via Swagger / OpenAPI documentation.**
  Rejected. Swagger is an API reference: it documents shapes, not
  system behaviour in time. A recent-events panel belongs to a dashboard
  that runs over the live system, not to an interactive schema browser.
  Mixing the two degrades both: observers lose the "what is happening
  right now" view, and API consumers lose a clean reference.

- **Drop the dashboard, rely on Actuator JSON + `curl` for demos.**
  Rejected. This is the status quo before PR 2 and it is worse for the
  primary use case (live demo, interview, walkthrough). A human
  observer watching JSON tick over in a terminal does not see the
  consistency window between command and projection; they see two
  walls of text. The dashboard makes that window visible.

## Consequences

### Positive

- The dashboard's role is documented. Future contributors know it is
  the primary observability surface, not a convenience layer, and know
  it carries a hard exit condition (500 LOC) rather than drifting.
- `/actuator/outbox-recent` adds real pipeline visibility with zero
  schema change and zero write-path change. The outbox schema stays
  unchanged; `aggregate_type` is derived at read time from the
  ArchUnit-enforced package convention
  (`...<aggregate>.domain.event.<Event>`).
- The events panel, command history, and search card each cover a
  concrete observability gap that Swagger and raw Actuator do not:
  *which* events flowed in the last N seconds, *what* the UI just did
  (and how long it took), and *can the read side resolve a title I
  just wrote?*.

### Negative / pending

- `/actuator/outbox-recent` is a public observability contract. The
  columns it reads — `id`, `event_type`, `occurred_at`, `processed_at`,
  `attempts`, `last_error` — become part of the surface the dashboard
  relies on. Removing or renaming any of them requires a superseding
  ADR, not a silent migration.
- The FQCN-based derivation of `aggregate_type` depends on the package
  convention enforced by ArchUnit today. A future refactor that moves
  events out of `<aggregate>.domain.event.*` would return "Unknown" in
  the dashboard. The derivation is unit-tested; a refactor that breaks
  the convention will fail those tests and force an explicit decision
  (either add a real column via Flyway or update the derivation).
- No tests on the dashboard file itself is a conscious trade-off. The
  prototype is visually reviewed and exercised during the same local
  flow that reviews the backend. The moment the dashboard moves into
  its own module, it gets its own test pyramid; until then, regressions
  land fast because the whole file is 500 LOC and reviewable in one
  sitting.

## Related work

- PR 2 (optional dashboard) introduced the file and established the
  "second-class citizen" framing that this ADR supersedes.
- PR 3 implements the repositioning: `OutboxRecentActuatorEndpoint`,
  the events panel, command history, and title search card.
- ADR 0003 (outbox pattern) is the reason the events panel can show
  per-event latency at all — `occurred_at` and `processed_at` are
  persisted on the same row, atomically with the aggregate write.
- When the dashboard graduates to its own module (likely React), a
  follow-up ADR supersedes this one at the "single static file"
  level and describes the module boundary. The `/actuator/outbox-recent`
  endpoint and its response shape remain; the UI layer is swappable.
