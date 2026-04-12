# Historical PR and Branch Structure

This note is a historical triage of unmerged work in `ostelco-core`, based on:

- the open pull requests in `ostelco/ostelco-core`
- the current remote branch graph after `git fetch --all --prune`
- local comparison of remote branches against `origin/develop`

Snapshot date: April 12, 2026.

The recommendations below are intentionally practical rather than sentimental: if this repository were revived, which old branches are worth learning from, which are worth re-implementing, and which are best left as history.

## At a glance

- `origin/develop` is the effective historical integration branch. All 10 open PRs still target `develop`.
- There are 10 open PR branches and 32 additional unmerged remote branches without an open PR.
- None of the currently open PR branches is stacked on top of another currently open PR branch.
- The open PRs are not a clean branch tree. They are mostly independent topic branches forked from different historical snapshots of `develop`.
- `origin/master` is itself divergent from `develop` (`ahead 38`, `behind 1725`), which reinforces that `develop` was the active line of integration.
- Several PR titles still say `[WIP]`, but they are open as ordinary PRs rather than GitHub draft PRs.

## Developer activity patterns

Looking just at the branch-only commits that are not already in `develop`, a few author patterns stand out:

- Bjørn Remseth appears across the documentation branches and much of the ES2+/SM-DP+, SIM-manager, and operational experimentation work.
- `mpeterss` is concentrated around DIAMETER, `ocsgw`, and charging/gateway internals.
- Kjell M. Myksvoll is the clearest owner of the recurring-payments branch and also appears on SIM/HSS admin work.
- Vihang Patil appears on Neo4j/tooling, build/container, Jumio, and some `ocsgw`-operations branches.
- Prasanth Ullattil shows up mainly in deployment, TLS, and concurrency-related experiments.

That does not prove ownership in a formal sense, but it is useful context: some branches are clearly one-person spikes, while others sit inside a broader contributor's domain of work.

## Git structure summary

From a git point of view, the open PR branches fall into a few clear ancestry bands:

| Fork point relative to current `develop` | What it means | Branches |
| --- | --- | --- |
| `behind 0` | Based directly on the current tip of `develop`; easiest to revive or cherry-pick. | `feature/readme-updates` (PR #988), `feature/diameter-parser` (PR #982), `feature/gource` (PR #985), `feature/ocsgw-kotlin-refactor` (PR #984), `feature/retry-logic-in-es2plus-client` (PR #964) |
| `behind 22` | Slightly stale but still recognizably in the same era of the codebase. | `feature/neo4j-backup` (PR #981), `feature/payment-recurring` (PR #914) |
| `behind 61` | Meaningfully stale and likely to need careful rebasing or re-implementation. | `feature/poll-smdpplus-for-install-and-download-statuses` (PR #971) |
| `behind 490-496` | Deeply stale WIP branches from a much older architecture snapshot. | `feature/ocs-coroutines` (PR #940), `feature/multiDataSource` (PR #843) |

Two practical conclusions follow from that:

1. There is no open-PR sub-branch chain to preserve. Reviving one PR does not require reviving another open PR first.
2. The real split is not parent/child branches, but freshness: a handful of small branches are still close to `develop`, while others are historical design sketches from much earlier phases of the project.

## Open PR assessments

These are the high-confidence findings, because each item has both a branch and an open PR describing the intended change.

| PR | Branch | Developers | Git state | Intended change | Revival assessment |
| --- | --- | --- | --- | --- | --- |
| [#988](https://github.com/ostelco/ostelco-core/pull/988) | `feature/readme-updates` | Bjørn Remseth | `ahead 3 / behind 0`, mergeable | Small documentation branch adding root README improvements and a `sim-administration/README.md`. | Worth keeping, but only selectively. This is clearly a one-person docs branch; the root README part is now partly superseded by newer documentation work, but the `sim-administration` README addition may still be worth cherry-picking. |
| [#971](https://github.com/ostelco/ostelco-core/pull/971) | `feature/poll-smdpplus-for-install-and-download-statuses` | Bjørn Remseth | `ahead 26 / behind 61`, not currently mergeable | Adds a polling task to ask the SM-DP+ for install/download status when ES2+ callbacks are unreliable, plus SIM manager and emulator changes. | Worth revisiting if eSIM provisioning is ever brought back. The branch sits squarely in Bjørn Remseth's SIM/eSIM area and addresses a real operational problem, but it is old enough that it should be rebased or re-implemented, not merged blindly. |
| [#982](https://github.com/ostelco/ostelco-core/pull/982) | `feature/diameter-parser` | `mpeterss` | `ahead 4 / behind 0`, mergeable | Fixes DIAMETER AVP type logging so common derived types from RFC 6733 are shown correctly. | Worth reviving. It is a focused DIAMETER/gateway branch by the contributor most associated with that area, and it is close enough to `develop` to be a good cherry-pick candidate. |
| [#985](https://github.com/ostelco/ostelco-core/pull/985) | `feature/gource` | Bjørn Remseth | `ahead 3 / behind 0`, mergeable | Adds scripts and docs for generating a Gource animation of the repository history. | Better forgotten unless someone explicitly wants a historical/demo artifact. This reads like a one-person documentation extra rather than product work. |
| [#984](https://github.com/ostelco/ostelco-core/pull/984) | `feature/ocsgw-kotlin-refactor` | `mpeterss` | `ahead 2 / behind 0`, mergeable | Large-scale port of `ocsgw` from Java to Kotlin, including tests. | Only worth revisiting if a Kotlin migration of `ocsgw` is itself a goal. It is a cleanly owned gateway refactor, but still a broad rewrite with little obvious user-facing payoff. |
| [#964](https://github.com/ostelco/ostelco-core/pull/964) | `feature/retry-logic-in-es2plus-client` | Bjørn Remseth | `ahead 6 / behind 0`, mergeable | Adds retry logic and cleanup in the ES2+ client to tolerate slow responses. | Worth reviving if the ES2+/eSIM path still matters. This is another small branch in the same SIM/eSIM domain, and likely easier to reintroduce than to rediscover under outage pressure. |
| [#981](https://github.com/ostelco/ostelco-core/pull/981) | `feature/neo4j-backup` | Vihang Patil | `ahead 2 / behind 22`, mergeable | Small tooling update for Neo4j backup/export support. | Worth revisiting only if Neo4j remains part of the live system and backup/export is still under-tooled. This looks like a narrowly owned ops/tooling branch rather than a cross-cutting product change. |
| [#914](https://github.com/ostelco/ostelco-core/pull/914) | `feature/payment-recurring` | Kjell M. Myksvoll | `ahead 55 / behind 22`, mergeable | Large recurring-payments feature spanning Stripe handling, APIs, graph storage, acceptance tests, and domain model changes. | Strategically worth revisiting if recurring billing is still a product requirement. This was clearly a substantial feature line with a strong primary owner, but it is broad enough that it should be treated as a design reference and source of ideas, not as a ready-to-merge branch. |
| [#843](https://github.com/ostelco/ostelco-core/pull/843) | `feature/multiDataSource` | `mpeterss` | `ahead 11 / behind 496`, not currently mergeable | Introduces an `ocsgw` datasource that can route different CCR types through different backends. | Conceptually interesting, especially if low-latency routing and fallback behavior still matter. It belongs to the same gateway/charging line of work as the other `mpeterss` branches, but it is far too stale to revive directly. Preserve the idea, not the implementation. |
| [#940](https://github.com/ostelco/ostelco-core/pull/940) | `feature/ocs-coroutines` | Prasanth Ullattil, `mpeterss`, Vihang Patil | `ahead 12 / behind 490`, not currently mergeable | Replaces `CountDownLatch`-style coordination with coroutines and `async`/`Deferred` in OCS and Neo4j paths. | Modest engineering value, but not a revival priority by itself. Unlike most of the open PRs, this one shows activity from several contributors, which suggests a shared refactoring thread rather than a single-owner spike. Even so, it should only be mined for ideas if those paths are touched again. |

## Recommendations by value if the project were revived

If the repository ever becomes active again, the historical work breaks down roughly like this:

### Strong candidates to revive or mine immediately

- PR #982 `feature/diameter-parser`
- PR #964 `feature/retry-logic-in-es2plus-client`
- PR #971 `feature/poll-smdpplus-for-install-and-download-statuses`
- PR #914 `feature/payment-recurring`
- PR #981 `feature/neo4j-backup`

### Worth mining for ideas, but not merging as submitted

- PR #984 `feature/ocsgw-kotlin-refactor`
- PR #843 `feature/multiDataSource`
- PR #940 `feature/ocs-coroutines`
- PR #988 `feature/readme-updates`

### Low-value historical artifacts

- PR #985 `feature/gource`

## Other unmerged remote branches without an open PR

This section is lower-confidence because these branches do not have an open PR describing intent. The summaries are inferred from branch names, top commits, and git placement relative to `develop`.

| Branch | Developers | Git state | Likely intent | Recommendation |
| --- | --- | --- | --- | --- |
| `feature/support-repos-incorporation` | Bjørn Remseth | `ahead 5 / behind 0` | Pull support-repository material into this monorepo. | Revisit only if repo consolidation is still a goal. Otherwise archive mentally and move on. |
| `feature/add-crud-to-hss-and-sim-vendor-tables` | Kjell M. Myksvoll | `ahead 3 / behind 38` | Add CRUD management for HSS and SIM/profile-vendor tables in the SIM admin area. | Worth revisiting if SIM operations return; small enough to re-do cleanly. |
| `feature/horde` | Bjørn Remseth | `ahead 7 / behind 22` | Experimental external authorization / IMSI-list workflow. | Only revisit with a specific product need; otherwise too unclear to prioritize. |
| `feature/efficient-analytics` | Cyriaque Brousse | `ahead 2 / behind 733` | Improve analytics efficiency, probably around cost or processing layout. | The idea may still matter, but the branch is too old; revive the concept, not the code. |
| `feature/github-action-stale-pull-requests-annotator` | Bjørn Remseth | `ahead 2 / behind 733` | Automate stale-PR handling in GitHub. | Low value for a dormant repo; not worth revival. |
| `feature/expose-legacy-api-to-graphql-schema` | Håvard Norén | `ahead 18 / behind 1314` | Surface existing APIs through the GraphQL layer. | Revisit only if GraphQL becomes a real product interface again. |
| `feature/opentrace-dropwizard-experiment` | Bjørn Remseth | `ahead 6 / behind 1319` | Trace/Jaeger experiment for Dropwizard services. | Better forgotten; even its own commit messages suggest the experiment was not promising. |
| `feature/debug-smdp-plus-conn` | Vihang Patil, Bjørn Remseth | `ahead 3 / behind 1528` | Temporary branch to debug SM-DP+ connectivity. | Forget. The top commit literally says `DO NOT MERGE`. |
| `feature/es2plus-healthcheck` | Bjørn Remseth | `ahead 2 / behind 1598` | Add health checking and logging around the ES2+ integration. | Worth redoing if eSIM provisioning returns; not worth reviving directly. |
| `feature/refactoring-of-sim-manager-based-on-hints-from-intellij-etd` | Bjørn Remseth | `ahead 3 / behind 1658` | Small readability/style refactors in the SIM manager. | Not worth reviving alone; fold into future cleanup if that area is touched. |
| `increase-prime-prod-resources` | Sami Alajrami | `ahead 2 / behind 1662` | Increase Kubernetes resources for Prime in production. | Historical ops tweak, not a code revival candidate. |
| `feature/ocsgw-json-logging` | Vihang Patil | `ahead 2 / behind 1694` | Add structured JSON logging to `ocsgw`. | Worth redoing if `ocsgw` is revived; observability improvement, but easy to implement fresh. |
| `feature/jumio-at` | Vihang Patil | `ahead 1 / behind 1734` | Acceptance test coverage for Jumio. | Worth revisiting if Jumio/eKYC remains relevant. |
| `feature/ocsgw-deployment` | merge-only / unclear | `ahead 1 / behind 1795` | Deployment work for `ocsgw`. | Redo from scratch if `ocsgw` needs modern deployment again. |
| `feature/sonarcube` | Bjørn Remseth | `ahead 13 / behind 1942` | SonarQube integration and quality reporting. | Only worth revisiting if CI and quality gates are being rebuilt. |
| `feature/rar-vendor-id-fix` | `mpeterss` | `ahead 7 / behind 2011` | Telecom protocol fix related to vendor-id handling. | Keep as a reference if the bug resurfaces; too stale for direct revival. |
| `feature/sim-inventory-maintenance-batchjob` | Bjørn Remseth | `ahead 5 / behind 2181` | Batch maintenance work around SIM inventory. | Worth rethinking if SIM inventory operations return. |
| `feature/distroless-docker-base-images` | Vihang Patil | `ahead 5 / behind 2278` | Move services to distroless or hardened base images. | Sensible idea, but redo fresh with current container practices. |
| `feature/hss-adapter-interface` | Bjørn Remseth | `ahead 15 / behind 2278` | Introduce an abstraction layer for HSS integration. | Conceptually worth revisiting if multiple HSS backends still matter. |
| `feature/ocsgw-ktc` | `mpeterss` | `ahead 2 / behind 2758` | Early Kotlin/coroutines work around `ocsgw` gRPC integration. | Historical precursor to the bigger Kotlin refactor, not a separate revival target. |
| `remove-hardcoded-filenames` | Sami Alajrami | `ahead 1 / behind 3002` | Remove hard-coded credential or config file names. | Small cleanup that may still be worth redoing if the issue persists. |
| `update-client-api-spec` | Håvard Norén | `ahead 1 / behind 3002` | Update the client API specification file. | Only worth reviving if that spec is still manually maintained and still used. |
| `feature/lets-encrypt-attempt-1` | Prasanth Ullattil | `ahead 9 / behind 3078` | Early TLS / Let's Encrypt deployment attempt. | Historical ops experiment; better forgotten. |
| `feature/esim` | merge-only / unclear | `ahead 1 / behind 3597` | Broad eSIM work from an older phase of the project. | Too vague and too old to revive directly; use only as archaeology. |
| `feature/jdk11-tmp-fix-for-jboss-repo` | Prasanth Ullattil | `ahead 3 / behind 3716` | Build/dependency workaround related to JDK 11 and old JBoss repository usage. | Very likely obsolete. Forget unless a build archaeology session proves otherwise. |
| `feature/tls-proxy` | `mpeterss` | `ahead 3 / behind 3728` | TLS proxy / gcloud deployment experiment around `ocsgw`. | Historical deployment work; redo if needed, do not revive directly. |
| `features/updating-coding-standards` | Bjørn Remseth | `ahead 1 / behind 4468` | Add a coding-standards note after a shell-script issue. | Only worth cherry-picking if the note itself is still useful. |
| `feature/prime-circle-ci` | Prasanth Ullattil, Vihang Patil | `ahead 2 / behind 4643` | Old CircleCI setup for building Prime. | Superseded by later tooling; not worth revival. |
| `feature/domain-model-documentation` | Bjørn Remseth | `ahead 15 / behind 4908` | Expand the domain-model documentation. | Keep as reference material, but do not merge unreviewed 2010s documentation into a revived repo. |
| `fix/proxy` | Martin Cederlof | `ahead 1 / behind 4918` | Bug fix to stop sending more than one CCA for a single CCR. | The bug idea may matter, but the branch is archaeological. Reproduce and re-implement only if the problem still exists. |
| `feature/ccr-terminate` | Martin Cederlof | `ahead 13 / behind 5060` | Change CCR termination / graceful shutdown behavior in the charging path. | Too old for direct revival. Only revisit if active telecom charging work resumes and the problem is still relevant. |
| `master` | merge-only / historical | `ahead 38 / behind 1725` | Historical release/mainline branch, no longer the active integration branch. | Preserve as history. Do not treat it as the branch to resume work from without a deliberate migration plan. |

## Bottom line

If this project were revived, I would treat the old PR and branch set as a backlog of design ideas rather than as a queue of merge-ready work.

The best historical material here is:

- small operational hardening around ES2+/SM-DP+ and DIAMETER visibility
- the recurring-payment feature as a product/design reference
- a handful of narrow tooling or documentation cleanups

The least valuable material is:

- repo vanity/documentation extras such as Gource output
- large rewrite branches whose main value is language migration rather than product behavior
- old CI, TLS, proxy, and deployment experiments that are easier to rebuild than to resurrect
