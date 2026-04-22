# 0010 — Dashboard LOC cap raised to 700, entity-snapshot joins the observability contract

**Status:** Accepted
**Date:** 2026-04-22

## Context

ADR 0009 promoted the dashboard to the primary observability surface
for the CQRS pipeline and set a hard 500 LOC cap on the single static
file, with an explicit exit condition: hitting the cap is the signal
that the dashboard has outgrown a single-file prototype and should
graduate to a separate frontend module with a build step.

Local review after PR 3 identified one more gap that belongs inside the
dashboard rather than deferred to a future module. The existing panels
show command flow, outbox counters, recent events, and a read-side
query, but nothing makes the *shape difference* between the write side
(Postgres, normalised with a `book_authors` join table) and the read
side (Mongo, denormalised with embedded `AuthorSummary` / `BookSummary`)
visible. That shape difference is the most concrete artefact a CQRS
demo can produce. Without it, the dashboard tells a complete story
about the pipeline but not about the reason the pipeline exists.

The fix is a per-entity side-by-side panel that fetches and renders
the Postgres row, the Mongo document, and the 10 newest event-log
entries for the currently-tracked author or book. Landing that panel
pushes the file to ~573 LOC, past the ADR 0009 cap.

Two honest paths forward: (a) graduate to a separate module now, as
ADR 0009 described — a large scope change for a single missing panel
while the information architecture is still moving; or (b) raise the
cap once, explicitly, with a new exit condition that does not drift.
This ADR chooses (b), acknowledges the precedent it sets, and tightens
the exit condition accordingly.

## Decision

### Changes to ADR 0009

- **LOC cap for `src/demo/src/main/resources/static/index.html` is
  raised from 500 to 700.** ADR 0009's other guarantees stand:
  no framework, no build step, no tests on the dashboard file itself.
- **The 700 cap is not a precedent for future raises.** The next
  attempt to exceed 700 triggers the ADR 0009 exit condition
  (separate frontend module with a build step) for real. This ADR
  does not grant an escalating budget; it grants one bounded
  exception to fit the entity-snapshot panel and closes the door
  behind it.

### New endpoint: `/actuator/entity-snapshot/{kind}/{id}`

- Sibling to `/actuator/outbox` and `/actuator/outbox-recent`.
- `kind` is `book` or `author`. `id` is a UUID string.
- Response shape: `{ postgres, mongo, events }`, each field
  independently nullable / empty. Any field can lag behind the
  others — especially `mongo` when the poller has not caught up
  with a just-written Postgres row.
- Implementation uses raw `NamedParameterJdbcTemplate` for Postgres
  and `MongoTemplate` for Mongo, deliberately bypassing the
  per-aggregate Spring Data adapters. Observability tooling reads
  the persistence layer directly; it does not route through a
  domain adapter. This keeps the per-aggregate onion intact
  (enforced by `ArchitectureTest`), matching the pattern already
  established by `OutboxRecentActuatorEndpoint`.

### Extension to the observability contract

ADR 0009 declared `/actuator/outbox` and `/actuator/outbox-recent` as
public observability contracts, with the rule that columns they read
(`event_type`, `occurred_at`, `processed_at`, `attempts`, `last_error`)
cannot be silently renamed or removed without a superseding ADR.

This ADR extends the same rule to the new endpoint:

- **Postgres:** `book_entity` (`id`, `title`, `deleted`, `version`),
  `book_authors` (`book_id`, `author_id`), `author_entity` (`id`,
  `first_name`, `last_name`, `birth_year`, `deleted`, `version`).
- **Mongo:** `books`, `authors`, `book_events`, `author_events`
  (document `_id`, plus `aggregateId`, `type`, `operation`,
  `timestamp` on the event collections).

Any rename or removal of the above requires a superseding ADR, the
same way a rename of `outbox.event_type` would.

## Alternatives considered and rejected

- **Keep the 500 cap, drop the entity-snapshot panel.** Rejected.
  The panel is the single feature that makes the CQRS shape
  difference concrete during a demo. Dropping it to preserve an
  arithmetic cap regresses the primary use case ADR 0009 was written
  to defend.

- **Keep the 500 cap, move the snapshot to a separate `/db.html`
  page.** Rejected. A separate page fragments the observability
  surface: the reviewer watches pipeline flow on `/`, then has to
  switch tabs to see what the pipeline actually produced. The main
  selling point of the dashboard — one screen, whole pipeline — is
  lost. The cost of the cap raise is lower than the cost of the
  fragmentation.

- **Graduate to a React (or similar) module now.** Rejected as
  premature. PR 3 alone added three panels and a new endpoint; the
  entity-snapshot panel adds a fourth. Locking a framework + build
  pipeline around a surface that is still moving buys nothing and
  slows iteration. The ADR 0009 exit condition (separate module)
  stays — it just does not fire on this particular PR. A future
  cap break does fire it, without further debate.

- **Reach into `book.infrastructure.jpa` / `book.infrastructure.mongo`
  repositories from the new endpoint, reusing existing
  Spring Data finders.** Rejected. ArchUnit's onion rule correctly
  flags this as a boundary break: the endpoint is not part of the
  Book or Author aggregate, so it has no business piercing their
  adapter layers from outside. Routing through raw `JdbcTemplate` +
  `MongoTemplate` is the right shape — matches
  `OutboxRecentActuatorEndpoint`, respects the onion, and makes
  the observability tooling's intent (inspect persistence, do not
  traverse domain) structurally visible.

## Consequences

### Positive

- The CQRS shape difference is now visible on the dashboard. A
  reviewer watching a live demo sees `book_entity` with
  `author_ids: [uuid]` next to the `books` document with
  `authors: [{authorId, fullName, retired}]`, plus the event log
  entries that carried one to the other — all in one screen, at
  the moment a command lands.
- The consistency window between a Postgres write and a Mongo
  projection catching up is explicit. When the panel shows
  `mongo: null (projection not caught up)` for a second or two
  after a command, the behaviour teaches eventual consistency more
  effectively than any diagram.
- The exit condition is tightened, not weakened. ADR 0009 said
  "500 is a hard cap"; this ADR says "700 is a harder cap: the
  next break is the module split, no further negotiations".

### Negative / pending

- One more precedent of raising the cap. The risk is clear: if
  the same rationale justifies raising it again, the dashboard
  drifts into a de-facto module with no build pipeline to enforce
  discipline. This ADR cannot guarantee the next decision, only
  its own boundary. A future ADR 0011 that proposes raising the
  cap further must confront the exit condition explicitly.
- Four more Mongo collections and two more Postgres tables
  (plus the `book_authors` join) are now part of the observability
  contract. Renames or removals require a superseding ADR. The
  domain team owns these schemas, so the constraint is real —
  though it aligns with the direction already established by the
  outbox endpoints.
- The dashboard still has no tests at the file level (unchanged
  from ADR 0009). The `/actuator/entity-snapshot` endpoint does
  have integration tests covering both stores, both kinds, both
  partial-population shapes, and the error paths.
- `birth_year` can be NULL on `author_entity`; the raw-JDBC read
  path handles this explicitly (`rs.wasNull()` guard) and returns
  `null` to the JSON response. Tested.

## Related work

- ADR 0009 — dashboard as first-class observability surface. This
  ADR amends its LOC budget line and extends its observability
  contract; all other decisions in 0009 stand.
- ADR 0003 — outbox pattern. The events rendered in the snapshot
  panel's third column come from the event log populated by the
  outbox-driven projection; without 0003 the panel would have no
  audit trail to show.
- `OutboxRecentActuatorEndpoint` — the precedent for bypassing
  per-aggregate adapters in observability code. Entity-snapshot
  follows the same rule.
- When (not if) the cap is challenged again, the response is the
  separate frontend module ADR 0009 already describes. A future
  ADR supersedes this one on the budget line at that point.
