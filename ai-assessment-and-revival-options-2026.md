# AI Assessment and Revival Options (2026)

This note is an AI-assisted assessment of `ostelco-core` as it exists on April 12, 2026. It is not a product plan. The goal is to answer a narrower question: if this repository were revived whole or in part, which parts still look strategically valuable, which parts look mostly historical, and how should a revival be approached in a 2026 toolchain and platform landscape?

## Short Answer

This repository still contains real telecom-specific substance. The parts that look most worth preserving are the custom DIAMETER and charging edge, the SIM and eSIM orchestration logic, and the domain model around subscribers, regions, plans, products, bundles, KYC, and support workflows. Those are the parts that would still be expensive to rediscover from scratch.

The parts that look least worth reviving unchanged are the old build and runtime stack, the legacy CI and deployment assumptions, the homegrown implementations of commodity platform concerns, and some of the older storage and observability glue. If the project were revived, the right approach would not be "turn the whole thing back on". It would be "extract the differentiated telecom core, replace the commodity shell, and modernize only what earns its keep".

## What Still Looks Valuable

### Telecom-specific edge logic

The clearest custom IP in the repo is the charging and telecom integration surface:

- [`ocsgw`](./ocsgw/README.md) and [`diameter-stack`](./diameter-stack/README.md) encode DIAMETER Gy-facing behavior and the translation into the internal OCS path.
- [`ocs-grpc-api`](./ocs-grpc-api/README.md) and [`ocs-ktc`](./ocs-ktc/) preserve the charging-side contracts and logic.
- [`seagull`](./seagull/README.md) and [`diameter-test`](./diameter-test/README.md) show that this was not just CRUD software; it had protocol-level test and load concerns.

If anything in this repository still justifies revival in 2026, it is here.

### SIM and eSIM operations

The SIM administration subtree still looks relevant if eSIM or operational provisioning ever matters again:

- [`sim-administration/simmanager`](./sim-administration/simmanager/)
- [`sim-administration/es2plus4dropwizard`](./sim-administration/es2plus4dropwizard/)
- [`sim-administration/hss-adapter`](./sim-administration/hss-adapter/)
- [`sim-administration/sm-dp-plus-emulator`](./sim-administration/sm-dp-plus-emulator/)

That code captures operational detail that is hard to rebuild from generic SaaS documentation alone.

### Business domain model and workflows

The model around customers, identities, regions, plans, products, bundles, subscriptions, purchases, KYC, and SIM profiles still looks like useful source material:

- [`model/src/main/kotlin/org/ostelco/prime/model/Entities.kt`](./model/src/main/kotlin/org/ostelco/prime/model/Entities.kt)
- [`customer-endpoint`](./customer-endpoint/README.md)
- [`admin-endpoint`](./admin-endpoint/README.md)
- [`customer-support-endpoint`](./customer-support-endpoint/)

Even if the runtime shape changed completely, those domain concepts would still be a good foundation.

## What Is Technically Stale

The repository is technically quite old in its current form:

- Root build uses Kotlin `1.3.61`, Java `12`, Gradle `6.0.1`, and a repository list that still includes JCenter, Bintray, and insecure HTTP Maven endpoints ([build.gradle.kts](./build.gradle.kts), [gradle/wrapper/gradle-wrapper.properties](./gradle/wrapper/gradle-wrapper.properties), [buildSrc/src/main/kotlin/org/ostelco/prime/gradle/Version.kt](./buildSrc/src/main/kotlin/org/ostelco/prime/gradle/Version.kt)).
- The main application stack is Dropwizard `1.3.17`, which is several major generations behind current Dropwizard.
- Observability uses OpenCensus, which is now a legacy path rather than the modern default.
- The root README still advertises Travis and CircleCI badges, which is a strong signal of historical CI assumptions.

## 2026 Modernization Baseline

If the Kotlin/JVM code is kept, a sensible 2026 baseline would look roughly like this:

| Area | In repo now | 2026 direction |
| --- | --- | --- |
| Java | 12 | Move to Java 21 first, or Java 25 once dependency compatibility is under control. As of April 12, 2026, both Java 21 and Java 25 are LTS releases. |
| Kotlin | 1.3.61 | Move to current Kotlin 2.3.x rather than trying to modernize business code on the old compiler line. |
| Gradle | 6.0.1 | Move to current Gradle 9.4.x. |
| Dropwizard | 1.3.17 | If staying on Dropwizard, target Dropwizard 5.x and accept the Jakarta and Java-baseline migration. |
| Observability | OpenCensus | Replace with OpenTelemetry. |
| CI/CD | Travis + CircleCI era assumptions | Consolidate on GitHub Actions and modern deployment pipelines. |
| GraphQL | GraphQL Java 13.0 | Upgrade only if GraphQL is still strategically important; otherwise consider dropping or rebuilding that surface. |
| Neo4j | Neo4j 3.5.x and driver 1.7.x | Upgrade only if graph storage still earns its keep; otherwise simplify the architecture. |

## Keep, Modernize, Replace, or Retire

| Area | Recommendation | Why |
| --- | --- | --- |
| `ocsgw`, `diameter-stack`, `ocs-grpc-api`, parts of `ocs-ktc` | Keep and modernize | This is the most differentiated telecom-specific code in the repo. |
| `sim-administration/*` | Keep selectively | Valuable if eSIM, HSS, SM-DP+, or SIM inventory workflows still matter. |
| `model` and domain concepts in `prime-modules` | Keep as source material | The domain language is reusable even if the runtime is rebuilt. |
| `prime` as one big modular Dropwizard host | Probably decompose over time | Reasonable as a starting point, but not ideal as the long-term 2026 shape. |
| `customer-endpoint`, `admin-endpoint`, `customer-support-endpoint` | Preserve behavior, not necessarily implementation | The workflows matter more than the exact Dropwizard-era wiring. |
| `payment-processor` | Replace most recurring-billing machinery with Stripe Billing, keep only telecom-specific fulfillment logic | Stripe now covers much of the generic subscription and invoicing surface more directly. |
| `auth-server` | Likely replace unless MSISDN-header auth remains a hard requirement | Issuing Firebase-backed tokens from a tiny custom service is only worth keeping if the network-edge identity model still depends on it. |
| `analytics-module` | Redesign | The current Pub/Sub to Dataflow to BigQuery notes reflect an older integration style and already describe cost and lineage pain. |
| `data-store` and `document-data-store` | Keep only as a transitional path | Firestore in Datastore mode is still valid when the Datastore API is a dependency, but it should be a conscious decision, not an inherited default. |
| `neo4j-store` | Re-justify before reviving | The graph model is elegant, but a dual-store architecture should only survive if it has operational value, not just historical value. |
| `tracing` | Replace | OpenCensus should not be part of a fresh revival. |
| `prime/infra` and older deployment scripts | Replace | The operational assumptions belong to an older platform era. |

## Scenarios Where Revival Could Still Make Sense

### 1. A small operator or MVNO needs custom telecom behavior

A full or partial revival could make sense if the business problem still genuinely requires custom online charging, SIM lifecycle management, KYC, regional product logic, and operator-specific support tooling. That is the strongest case for reuse.

### 2. The charging and DIAMETER edge is the main asset

If the real value is the Gy-facing side, it may make sense to revive only the charging gateway and OCS core as a narrower telecom platform, and rebuild the customer and admin surfaces around it using a more modern stack.

### 3. eSIM operations return as a priority

If the business differentiator is eSIM orchestration, SIM inventory, or SM-DP+ integration, the sim-administration subtree looks like a strong candidate for selective resurrection.

### 4. The repo is used as a domain-reference extraction project

Even if the old runtime is not revived directly, the code can still be useful as a source of business rules, API surface area, entity definitions, operational cases, and historical workflows for a replacement system.

## Scenarios Where Revival Probably Does Not Make Sense

Reviving the whole platform is probably a poor idea if the real need is only:

- standard subscription billing
- standard customer identity and login
- generic admin CRUD
- generic analytics export
- a modern app backend without telecom-specific protocol or provisioning requirements

In those cases, much of this repository would be an expensive historical detour.

## How a 2026 Revival Should Probably Happen

### Phase 1: Inventory before touching architecture

Use AI agents and scripted analysis to produce:

- a module and dependency map
- extracted OpenAPI and gRPC contract inventory
- a data model map
- an external dependency inventory
- a branch and PR salvage map
- a list of build blockers and dead dependencies

This repository is large enough that the first step should be understanding, not rewriting.

### Phase 2: Choose a narrow revival slice

Do not modernize the entire monorepo blindly. Pick one thin slice with clear business value:

- charging path
- eSIM path
- customer purchase path
- support/admin path

Then make that slice build, test, and run on a modern toolchain before expanding scope.

### Phase 3: Replace commodity infrastructure early

This is where a 2026 revival should be aggressive:

- replace legacy CI with GitHub Actions
- replace OpenCensus with OpenTelemetry
- replace old deployment assumptions with current Cloud Run, GKE, or other supported platform automation
- replace custom recurring-billing machinery with Stripe Billing where product fit allows
- replace old gateway and API-management assumptions with current OpenAPI-first gateway choices

### Phase 4: Preserve the telecom-specific core

This is where a revival should be conservative:

- DIAMETER parsing and protocol behavior
- charging semantics
- SIM and eSIM workflows
- regional telecom business rules
- tricky KYC and fulfillment flows

Those are the areas where "rewrite from scratch with AI" is least likely to be wise.

### Phase 5: Move toward a clearer architecture

The likely end state in 2026 is not one giant revived Dropwizard host. It is more likely:

- a smaller telecom core kept as Kotlin/JVM services
- clearer contracts between subsystems
- more managed cloud services for non-differentiating infrastructure
- fewer global singletons and fewer framework-era assumptions

## Agentic-AI-Heavy Revival Workflow

An AI-heavy development culture could help a lot here, but it should be used for acceleration, not wishful thinking.

Good uses of AI in a revival:

- dependency upgrade scaffolding
- extracting API contracts from code
- generating architecture maps and ADR drafts
- writing or modernizing tests around existing behavior
- searching for dead code, duplicate flows, and stale docs
- proposing decomposition seams between modules

Bad uses of AI in a revival:

- guessing telecom semantics
- rewriting charging behavior without strong tests
- changing KYC or payment workflows without domain review
- migrating production data models without human verification

The safe pattern is "AI for inventory, scaffolding, and mechanical modernization; humans for telecom correctness, security, compliance, and cutover decisions".

## References Checked For This Note

The current-platform recommendations above were checked against official sources on April 12, 2026:

- [Oracle Java SE support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Kotlin release process](https://kotlinlang.org/docs/releases.html)
- [Kotlin 2.3.20 release notes](https://kotlinlang.org/docs/whatsnew2320.html)
- [Gradle current release notes](https://docs.gradle.org/current/release-notes.html)
- [Dropwizard 5.0.x upgrade notes](https://www.dropwizard.io/en/stable/manual/upgrade-notes/upgrade-notes-5_0_x.html)
- [OpenTelemetry OpenCensus migration note](https://opentelemetry.io/docs/migration/opencensus/)
- [OpenTelemetry OpenCensus compatibility spec](https://opentelemetry.io/docs/specs/otel/compatibility/opencensus/)
- [Firestore: choosing Native mode vs Datastore mode](https://cloud.google.com/datastore/docs/firestore-or-datastore)
- [Cloud Run overview](https://cloud.google.com/run/docs/overview/what-is-cloud-run)
- [Google Cloud API Gateway documentation](https://cloud.google.com/api-gateway/docs)
- [Google Cloud API Gateway OpenAPI overview](https://cloud.google.com/api-gateway/docs/openapi-overview)
- [Stripe Billing documentation](https://docs.stripe.com/billing)
- [GitHub Actions documentation](https://docs.github.com/en/actions/)
- [GraphQL Java releases](https://github.com/graphql-java/graphql-java/releases)
- [Neo4j Java Driver manual](https://neo4j.com/docs/java-manual/current/)
