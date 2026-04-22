# 0008 — Authors are a separate aggregate, linked via a dedicated write operation

**Status:** Accepted
**Date:** 2026-04-22

## Context

`BookAggregate` used to carry `author` as a single `String` field on the
aggregate itself. The `CreateBookRequest` / `UpdateBookRequest` DTOs exposed
the same field, the commands carried it, the handlers wrote it, and the
read model stored it denormalised on `BookReadModel`. The "a book has one
author" shape was baked end-to-end across the write side and the read side.

That model broke as soon as the book domain had to represent real
bibliographic data: a book can have zero, one, or several authors; an
author writes many books; an author has a lifecycle of their own (rename,
retirement). Neither cardinality nor lifecycle fit into a flat string on
`BookAggregate`.

Migrations `V4__create_author_entity.sql` and
`V5__book_authors_relation.sql` introduced the new shape on the write side:
`Author` as its own aggregate and a `book_authors` join table. The
application was restructured around it: `CreateAuthorCommand`,
`RenameAuthorCommand`, `DeleteAuthorCommand` (soft delete), and on the book
side `AddAuthorToBookCommand` / `RemoveAuthorFromBookCommand`. Projections
were rewritten to denormalise author names into `BookReadModel.authors`
and to keep them in sync when an author is renamed or retired.

What was left unresolved during that migration was the `author` field on
`CreateBookRequest` and `UpdateBookRequest`. The field stayed in the DTO
while the command, the aggregate, and the handler stopped reading it. The
server silently dropped the value on the floor. From the outside the DTO
still advertised a capability the model no longer supported — a cleanly
passing request that changed nothing. This ADR documents the decision to
remove the field from the DTO and to commit to the two-aggregate shape as
the only supported way of associating an author with a book.

## Decision

Book creation accepts only `title`. Authors are managed as a separate
aggregate through the `Author` bounded slice, and `POST /books/{id}/authors/{authorId}`
is the sole supported operation for linking an author to an existing book.
The DTO, the command, the handler, and the tests are cleaned to match.

### Changes

- `CreateBookRequest` exposes only `title`. `UpdateBookRequest` exposes
  only `title`. Neither carries an `author` field.
- `CreateBookCommand` and `UpdateBookCommand` carry only `id`/`bookId` and
  `title`. The handlers do not read an author value.
- `BookAggregate` no longer holds a single-author field.
  `BookAggregate.create(id, title)` is the factory; `update(title)` is the
  sole mutator for the title.
- `BookReadModel.authors` is a list of `AuthorSummary` (id, full name,
  retired flag), populated and maintained by the projections:
  `AuthorAddedToBookMongoProjection`, `AuthorRemovedFromBookMongoProjection`,
  and `BookUpdatedAuthorDenormProjection` (which denormalises renames and
  retirements from the `Author` aggregate into the book read model).
- `AddAuthorToBookCommandHandler` requires the author to exist and to be
  non-retired before it will accept the link, enforced through
  `AuthorExistenceChecker.ensureExistsAndActive`. A missing author yields
  `HTTP 404`; a retired author yields `HTTP 409`.
- The client-facing flow to create a book with an author is three calls:
  `PUT /authors/{authorId}` (create the author), `PUT /books/{id}`
  (create the book), `POST /books/{id}/authors/{authorId}` (link them).
  Each call is idempotent on its own identifier.

### Why split the write

A single book-create request that also materialises authors would either
(a) require the client to send full `Author` objects and make the server
deduplicate them against existing authors by some server-chosen key, or
(b) require the client to invent author IDs ahead of time anyway, which is
indistinguishable from asking the client to call `PUT /authors/{id}` first.

Option (a) hides two writes behind one, collapses the transactional scope
of "create book" to include "maybe create one or more authors", and needs
a deduplication rule that has no obvious right answer (name collisions are
common in bibliographic data — "John Smith" is not one person). It also
smears the invariants of the `Author` aggregate across the book-creation
handler.

Option (b) is the status quo with one extra HTTP round trip hidden inside
the server. The benefit over the current flow is small; the cost is a
second command executing in the same transaction, surfacing partial-failure
modes that do not exist today.

The simpler design is honest: `Book` and `Author` are separate aggregates,
`book_authors` is their relation, and the association is its own write.
The DTO now reflects that.

### Alternatives considered and rejected

- **Accept an `authorIds` array on `CreateBookRequest`.** Rejected. The
  client must still create the authors separately before it knows their
  IDs — there is no batching upstream. Accepting an `authorIds` array on
  create saves exactly one HTTP round trip per new author, at the cost of
  a new transactional shape: one command writing into two aggregates, both
  of whose invariants must be enforced inside the same handler. The
  `AuthorExistenceChecker` contract stays uniform today (every add-author
  operation goes through the same path); batching would fork it. Not worth
  the asymmetry for a demo API.

- **Accept full `Author` objects on the book-creation request and
  upsert-on-create.** Rejected. Hides a second operation behind a "simple"
  create, pushes deduplication logic into the book handler, and makes the
  request schema a union type depending on whether the author is new or
  existing. The handler would need to distinguish intent ("I am creating
  this author right now" vs. "I am referring to this author") through
  heuristics on the payload, which is exactly the kind of implicit
  contract the CQRS split exists to avoid.

- **Keep the `author` field on the DTO as a deprecated, ignored hint.**
  Rejected. The DTO is the public contract. A field that the server
  silently ignores is worse than a breaking change — callers write code
  against it, their tests pass against it, and the drift is only caught
  when someone asks why the data is missing. The DTO must describe the
  model it actually talks to.

- **Move author linking into an implicit projection from a naming
  convention (e.g., extract author from title format).** Rejected on sight.
  Structured data should not be reconstituted from parsing conventions of
  free text. Raised only to state it clearly: the domain is not doing
  this.

## Consequences

### Positive

- The DTO tells the truth. `CreateBookRequest.title` is the only field; the
  field is required; the command and handler operate on exactly that input.
  A reader of the record understands what `PUT /books/{id}` does without
  reading the handler.
- Book creation is strictly about identity + title. It has no transactional
  dependency on the `Author` aggregate, so a misconfigured or unavailable
  author service does not block book creation, and a retired-author check
  never runs in the book-creation path.
- The association between a book and an author is an explicit, named
  operation (`AddAuthorToBookCommand`). Its preconditions — the author must
  exist and be non-retired — are enforced in one place. Failure modes are
  observable as their own events (`AuthorAddedToBookEvent`) and auditable
  through the outbox-driven audit projection.
- Authors are first-class and have their own lifecycle (rename, soft
  delete). `BookUpdatedAuthorDenormProjection` handles propagation into the
  book read model; rename and retirement are reflected end-to-end without
  touching `BookAggregate`.
- The decision is defensible at the modelling level: `Book` and `Author`
  are separate aggregates because they have independent lifecycles and
  independent invariants. Linking them is a relation, not a field.

### Negative / pending

- A client that wants to create a book with a pre-existing author pays two
  HTTP round trips (`PUT /books/{id}`, then `POST /books/{id}/authors/{authorId}`),
  and three if the author is also new. For a UI driven by a human this is
  fine; for a bulk-import client this is latency that adds up. If bulk
  ingestion ever becomes a use case, the fix is a dedicated
  `ImportBookCommand` that accepts a denormalised payload and fans out
  internally — not weakening the `CreateBookCommand` contract.
- Consumers of previous, deprecated `CreateBookRequest` payloads that still
  include `author` will silently have that field dropped by Jackson. That
  is intentional at the `@Valid` layer but does mean the migration is a
  contract breaker at the schema level. Acceptable: there is no public
  client, no external consumer, and the field never had working behaviour
  behind it to begin with.
- Projection logic for author-name denormalisation
  (`BookUpdatedAuthorDenormProjection`) is load-bearing. A bug in it
  shows up as stale author names in the book read model. Covered by
  `BookWithAuthorsLifecycleIntegrationTest` (rename propagation,
  retirement propagation, remove-author propagation). Any future change
  to the author-name shape requires re-verifying these paths end-to-end.

## Related work

- Migrations `V4__create_author_entity.sql` and
  `V5__book_authors_relation.sql` established the persistence shape this
  ADR commits to at the contract level.
- `BookWithAuthorsLifecycleIntegrationTest` covers the end-to-end flow:
  create author, create book, link, rename, retire, remove. It is the
  executable spec for the decision recorded here.
- Follow-up (not an ADR commitment): if/when OpenAPI publication lands
  (Day 16+), the three-call flow is the shape documented in the API
  spec. Any later proposal to add an author field back onto a book DTO
  — e.g., for bulk import — gets its own ADR that supersedes parts of
  this one explicitly.
