# 15. Publish core as a versioned Maven artifact on GitHub Packages

Date: 2026-04-24

## Status

Accepted

## Context

`core` is designed to be framework-agnostic. ArchUnit enforces that
`core.contracts` and `core.ddd` carry no dependency on
`org.springframework..`, and `demo-vanilla` demonstrates — by example —
that the same `core` jar runs outside a DI container. The next step of
the portability thesis is a second and third adapter consuming the same
`core` from a **separate repository** (candidates: Micronaut, Quarkus).
A consumer repo that declares `core` as a Maven dependency is a stronger
proof of portability than a sibling module inside the same repo: the
reviewer sees the library consumed at arm's length, not copied in.

For that, `core` must be publishable as a versioned Maven artifact.

State of the repo immediately before this ADR:

- Reactor, `core`, `demo` and `demo-vanilla` were all at version `1.0.0`
  in their POMs, while git tags had progressed through `v1.0.0`,
  `v1.1.0`, `v1.2.0`, `v1.3.0`. The POMs were never bumped — each
  internal release moved only the tag. This historical drift is
  harmless while nothing is published, but hostile once a consumer
  exists: "which version am I looking at?" stops being answerable by
  either source.
- `demo` and `demo-vanilla` declared `com.oscaruiz:core:1.0.0` with
  hard-coded versions, so any bump of `core` required bumping three
  places in lockstep.

## Decision

Publish `core` on **GitHub Packages** as `com.oscaruiz:mycqrs-core`,
versioned in lock-step with the repo's single version axis.

Operational rules:

- **Single version axis.** Reactor, `core`, `demo` and `demo-vanilla`
  share the same version. One tag, `v<version>`, labels the repo as a
  whole. Consumers who depend on `mycqrs-core:X.Y.Z` can always find
  the matching `demo` source at `git show v X.Y.Z`.
- **First public release: `1.3.1`.** Patch on top of `v1.3.0`. The
  contents of this release are the publishing infrastructure itself
  (flatten plugin, distribution management, source/javadoc jars, CI
  workflow, this ADR, README section). No API changes. The POM drift
  against the tag line is closed by bumping the POMs to `1.3.1`
  instead of rewriting `v1.3.0`.
- **Permanent rule from here on: `reactor.version == next published
  tag`.** Each PR that intends to publish bumps every POM in advance.
  Drift is detectable by CI; that check is a follow-up (see §Known
  follow-ups in the plan file).
- **Tag-triggered publishing.** A dedicated workflow
  (`.github/workflows/publish-core.yml`) fires on `push: tags: 'v*'`
  and runs `./mvnw -pl src/core -am deploy`. Merging to `main` never
  publishes; only tagging does.
- **Flatten the published POM.** `flatten-maven-plugin` in `oss` mode
  strips the `<parent>` reference before the POM is uploaded, so
  consumers do not need access to the reactor. This was discovered
  during the local smoke test: without flatten, Maven fails to
  resolve `com.oscaruiz:mycqrs:1.3.1` because only `core` is
  published.
- **Strict semver over `core.contracts` and `core.ddd`.** These are
  the public surface; breaking changes there imply a major bump of
  the whole repo. Changes localized to `demo` or `demo-vanilla`
  still bump the repo — this is an accepted cost of the single-axis
  model and is documented below.

## Alternatives considered

- **JitPack.** Rejected. Forces coordinates of the form
  `com.github.oscaruiz:mycqrs-core`, which leaks the hosting provider
  into every consumer. GitHub Packages keeps coordinates under
  `com.oscaruiz`, the namespace the project already uses in Java
  packages. Also one less third-party dependency in the publication
  path.
- **Maven Central via Sonatype OSSRH.** Rejected. Requires GPG signing,
  staging repositories, and `com.github.*` or `io.github.*` group
  verification. Disproportionate ceremony for a portfolio project
  whose consumers will be sibling repos under the same GitHub account,
  not the general public. Reconsider if the library develops an
  external user base.
- **Monorepo with adapters as sibling modules** (`demo-micronaut`,
  `demo-quarkus` inside `src/`). Rejected because it undermines the
  very thesis the adapters are meant to prove. The argument
  "`core` is portable because a second adapter consumes it" is
  materially weakened when both live in the same repo sharing a
  reactor: the reviewer cannot distinguish "portable library" from
  "shared code via Maven reactor" without reading the POMs. A
  sibling-repo consumer forces the library to stand on its published
  surface alone.
- **Two version axes** (core with its own version, reactor with its
  own, demos with theirs). Rejected. Adds cognitive overhead — "which
  version of what am I on?" — for no benefit while `core` and the
  demos evolve together. Revisit if they ever genuinely diverge in
  release cadence.
- **Keep the reactor at `1.0.0` and start publishing at `1.0.0`.**
  Rejected. Forces an explanation of why the library is "resetting"
  to `1.0.0` after three historical tags already carried the project
  forward. Worse narrative, no upside.
- **Re-tag `v1.3.0`** so the first published version coincides exactly
  with the highest existing tag. Rejected. Requires a force push on a
  shared tag, which is destructive and leaks into anyone who has
  already fetched. `1.3.1` as a patch achieves the same narrative
  (first published release is in the 1.3 line) without touching
  history.
- **Enforce "no parent" on `core` directly** instead of flattening.
  Rejected. The reactor is useful for dev ergonomics (shared
  `dependencyManagement`, plugin versions, enforcer rules). Removing
  the parent would duplicate those across every module. The flatten
  plugin resolves the parent at package time while keeping the
  development reactor intact — best of both.
- **Publish the reactor POM too** so consumers can resolve the parent.
  Rejected. The reactor is a build-time aggregator, not a library.
  Publishing it would confuse consumers about which artifact to
  depend on and embed `demo` metadata in the public channel.

## Consequences

**Positive**

- `v1.3.1` becomes the first version that identifies a specific,
  reproducible release of the whole repo — reactor, `core`, `demo`
  and `demo-vanilla` simultaneously. The git tag, the POM version,
  and the artifact coordinate all agree.
- The published POM is self-contained. A consumer only needs the
  GitHub Packages repo plus a PAT with `read:packages`. No access
  to the source repo, no awareness of the multi-module layout.
- Adapters in separate repositories become possible. The portability
  thesis becomes executable at the dependency-resolution layer, not
  just inside the source tree.
- Release process is automated by tag push. There is no manual
  `mvn deploy` that can be executed with the wrong version.

**Negative / accepted costs**

- **GitHub Packages requires a PAT even for public packages.** This
  is a known GitHub limitation, not a project choice. The README
  documents this so consumers can set up `~/.m2/settings.xml` without
  hunting. If this friction proves prohibitive, re-evaluate
  Sonatype OSSRH for a future major.
- **Single axis couples release cadence.** A change in `demo` that
  does not touch `core`'s public API still bumps the whole repo and
  therefore the published `core` version. Accepted: release cadence
  is low enough that the noise is bearable, and the narrative
  simplicity of "one version == one snapshot of the whole repo"
  outweighs the cost. Revisit if `demo` starts needing its own
  release cycle.
- **Semver discipline now has teeth.** Breaking changes in
  `core.contracts` or `core.ddd` are visible to downstream consumers.
  This is a benefit framed as a cost: it forces the public surface to
  be evolved deliberately.
- **`reactor.version == next tag` is a manual rule today.** Nothing
  in CI enforces it. The "Known follow-ups" section of the plan file
  captures a proposal for a CI step that compares the reactor POM
  against `git describe --tags --abbrev=0`; that enforcement is a
  Phase 2 ADR of its own.

## Related work

- **ADR 0007 (immutable Docker images via GHCR)**: analogous
  publication infrastructure for the runtime image of `demo`. This
  ADR extends the same "GitHub-hosted publication" pattern to the
  library artifact.
- **ADR 0013 (vanilla adapter demonstrating framework-agnostic
  core)**: the direct motivation. Without a published `core`, the
  follow-up adapter repos cannot exist as independent consumers.
- **ADR 0014 (symmetric QueryInterceptor pipeline, pending)**:
  unrelated in subject, but it reserves the number 0014. This ADR
  therefore takes 0015.

## Notes for future revision

- If `core` is extracted to its own repository, this ADR is superseded
  by one that describes the independent release cadence and may
  finally introduce a second version axis.
- If the first external consumer proves that PAT friction kills
  adoption, evaluate publishing to Maven Central under `io.github.*`
  as a parallel channel, not a replacement.
