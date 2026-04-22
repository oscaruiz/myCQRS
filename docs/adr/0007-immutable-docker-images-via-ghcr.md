# 0007 — Immutable Docker images published to GHCR by CI

**Status:** Accepted
**Date:** 2026-04-21

## Context

The project is being moved from "runs on the developer's laptop" to "runs as
a public live demo on Render free tier, fronted by a Neon Postgres and an
Atlas MongoDB M0 cluster". The purpose of the deployment is to let readers of
the repo — potential reviewers, interviewers, or anyone following an ADR
link — click through to a real URL and see `/swagger-ui.html`,
`/actuator/health`, `/actuator/outbox` responding against a real stack.

That purpose imposes three constraints on the deployment pipeline:

1. **Reproducibility.** The artifact that CI tested must be bit-identical to
   the artifact that production runs. A demo where the live environment
   silently drifts from what the README says is worse than no demo.
2. **Fast rollback.** Free-tier Render has no blue/green, no canary, no
   pre-provisioned traffic splitting. Recovery from a bad deploy means
   "point the service at the previous artifact and restart". That only works
   if previous artifacts are addressable and immutable.
3. **Cheap operation.** Render free tier is the target. The pipeline must not
   require paid GitHub Actions minutes tiers or a second CI system. One
   GitHub repository, one workflow file, one free registry.

GitHub Actions already runs `./mvnw verify` on push in `ci.yml`. GHCR is
available to the same repository with no additional account, no rate limits
comparable to Docker Hub's, and authenticatable with the ambient
`GITHUB_TOKEN` — no PAT rotation to manage. Render supports "deploy from
registry" with a deploy hook URL and a container tag: no source build on
their side.

The ingredients for a reproducible, rollback-friendly pipeline are already
in the ecosystem. The decision is how to assemble them.

## Decision

CI is the sole authority on what runs in production. On every push to
`main`, `.github/workflows/deploy.yml`:

1. Runs `./mvnw verify` (Testcontainers Postgres + Mongo, full test suite).
2. Logs into GHCR with the ambient `GITHUB_TOKEN`.
3. Builds the Docker image from the existing multi-stage `Dockerfile`.
4. Pushes two tags:
   - `ghcr.io/${{ github.repository_owner }}/mycqrs:${{ github.sha }}` — an
     immutable, content-addressable reference to this specific commit.
   - `ghcr.io/${{ github.repository_owner }}/mycqrs:latest` — a moving
     pointer kept for Render's convenience.
5. POSTs the `RENDER_DEPLOY_HOOK` secret URL, which instructs Render to pull
   the new `:latest` tag and restart the service.

Render is configured out-of-band (manually, in the Render dashboard) to
pull the image by tag. Rollback is a one-field change in the Render UI:
point the service at any historical `<sha>` tag. The demo is back to the
previous known-good state in seconds, with no rebuild involved.

No source code lives on Render's build side. Render orchestrates; it does
not compile.

### What is in scope

- `.github/workflows/deploy.yml` is authored in this ADR.
- The existing `Dockerfile` is reused verbatim (multi-stage, non-root user,
  `SPRING_PROFILES_ACTIVE=prod`, JVM flags tuned for 512 MB containers).
- `RENDER_DEPLOY_HOOK` is a GitHub repository secret; its value is not
  committed anywhere in the repo.

### What is out of scope

- The Render service itself, the Neon database, and the Atlas cluster are
  provisioned manually by the repository owner. This ADR does not claim
  authority over them.
- Secret values — `RENDER_DEPLOY_HOOK`, `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
  `SPRING_DATA_MONGODB_URI` — are set in Render's environment dashboard.
  None of them appear in code, comments, or workflow files.

## Alternatives considered and rejected

- **Render-native "build from source" mode.** Rejected. Render would check
  out `main` on each deploy and run its own builder. Two failure modes:
  (1) the build is not reproducible — a later build could pull different
  transitive dependency versions if any of our resolved versions have
  drifted, producing an artifact CI never saw; (2) rollback requires
  re-building an old commit, which multiplies the reproducibility risk
  exactly when you least want it (during an incident). Keeping the builder
  in CI and shipping the output is a one-time cost that buys us both
  properties.

- **Docker Hub instead of GHCR.** Rejected. Docker Hub's free-tier anonymous
  pull rate limits create a tail risk we do not need: if Render's egress IP
  is shared with noisy neighbors, deploys can fail with
  `toomanyrequests`. GHCR is attached to the same repo, has generous limits
  for public images, and authenticates with `GITHUB_TOKEN` we already own.
  No second vendor relationship to maintain.

- **Build locally and `docker push` from the developer machine.** Rejected.
  The entire point is that the artifact CI tested matches production. Local
  builds depend on the developer's toolchain state and cannot be audited
  after the fact. This would also entangle the deploy with whatever branch
  the developer happened to have checked out.

- **Only the `latest` tag, no per-SHA tag.** Rejected. Without the SHA tag,
  there is no stable identifier for "the image that shipped on commit X".
  Rollback becomes "rebuild from the older commit" — which has the
  reproducibility problem above. The SHA tag is cheap (zero extra push cost
  thanks to layer caching) and is the entire reason this pipeline is a
  rollback-in-seconds story instead of a rebuild-and-hope story.

- **Only the per-SHA tag, no `latest`.** Rejected for pragmatic reasons.
  Render's "deploy from registry" UI is easier to wire against a stable tag
  name for the happy path. `latest` as a moving pointer is a convention
  Render expects. The SHA tag is the source of truth for rollback; `latest`
  is the convenience pointer for forward deploys.

- **Skip `./mvnw verify` in the deploy workflow because `ci.yml` already
  ran it.** Rejected. On a push to `main`, both workflows start in parallel.
  Nothing guarantees `ci.yml` has finished — or passed — by the time the
  deploy job reaches its build step. Running verify again in the deploy
  workflow makes the pipeline self-contained: if deploy is green, the tests
  were green against *this* checkout. The cost (a few extra minutes on
  `ubuntu-latest`) is trivial for a free-tier demo.

## Consequences

### Positive

- The image that runs in production is the exact image whose layer digests
  CI recorded. Reproducibility is bit-identical, not "probably the same
  dependency tree".
- Rollback is a single tag change in the Render dashboard. No rebuild, no
  waiting for CI, no risk of transitively different dependencies.
- Render has no credentials for the source repository. Its attack surface
  toward our code is zero: it pulls an image and runs it.
- The `RENDER_DEPLOY_HOOK` is the only platform-specific integration point,
  and it is referenced by name only. Swapping Render for another container
  host (Fly, Railway, a self-hosted Kamal target) is a two-line change.
- `GITHUB_TOKEN` avoids a PAT to rotate. One fewer secret lifecycle.

### Negative / pending

- Two full runs of `./mvnw verify` per merge to `main` (once in `ci.yml`,
  once in `deploy.yml`). The Testcontainers warm-up is the dominant cost;
  total wall-clock added is on the order of a few minutes. Acceptable for
  a free-tier demo. If the project ever outgrows this, the fix is to
  restructure `ci.yml` and `deploy.yml` around `workflow_run` dependencies
  so the deploy reuses the CI's passing state instead of re-running it.
- GHCR storage quota is generous but not unlimited. If the project is still
  deploying on this setup a year from now, a retention policy on old SHA
  tags becomes warranted. Not a near-term concern.
- `latest` is a moving pointer, and moving pointers are a known footgun.
  This setup tolerates it because (a) SHA tags are the rollback reference,
  and (b) Render's deploy hook is the only thing that consumes `latest`,
  and it consumes it on an explicit POST, not a watch.
- The initial GHCR image visibility defaults to private. The repository
  owner flips it to public once in the GHCR UI after the first successful
  push. Not automatable from a workflow without elevated credentials, and
  not worth introducing those for a one-time step.

### Post-deploy validation

The deploy workflow polls `/actuator/health` against the Render-hosted
service after triggering the deploy hook, and fails the workflow if the
new container does not report `"status":"UP"` within ~5 minutes. The
deploy hook itself returns 200 as soon as Render *queues* the pull; it
does not confirm that the new image actually starts. Without this
post-deploy check, a broken image could ship while the workflow stayed
green and the first user would be the one to discover the outage. The
smoke test closes that gap at the cost of one additional repo secret
(`RENDER_APP_URL`) and ~5 minutes of worst-case workflow time — paid
only on deploys that actually hang, since the loop exits on first
success.

### Runtime base image: Debian, not Alpine

The runtime stage uses `eclipse-temurin:21-jre` (Debian-based, glibc) rather
than `eclipse-temurin:21-jre-alpine` (musl). This is deliberate and driven
by third-party TLS compatibility, not image-size preference: Alpine's musl
libc combined with its minimal CA truststore fails the TLS handshake
against MongoDB Atlas shard nodes with `Received fatal alert:
internal_error`, even though the Postgres/Neon JDBC driver connects from
the same container without issue. The Debian-based JRE carries the full
CA bundle and a glibc resolver that handle Atlas' SNI/TLS negotiation
correctly. The cost is ~80 MB of extra image size, which remains well
within Render free tier's 512 MB pull budget. If the runtime image is
ever swapped for a smaller base, Atlas connectivity must be re-verified
end-to-end before merging.

## Related work

- ADR 0003 (outbox pattern): this ADR does not touch the runtime behavior of
  the outbox, but the `/actuator/outbox` endpoint introduced alongside the
  deployment makes the outbox visible in prod — the health narrative
  (`pending` drains to `processed`) is the headline demo.
- ADR 0004 (optimistic locking): unchanged; mentioned for completeness
  because `409` responses on concurrent `PATCH` are part of what the live
  demo showcases.
- Follow-up (not an ADR commitment): once the demo is live and stable,
  consider restructuring `ci.yml` and `deploy.yml` around `workflow_run`
  to avoid the double-verify, and adding a retention policy for old GHCR
  tags.
