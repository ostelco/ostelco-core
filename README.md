
[![Kotlin version badge](https://img.shields.io/badge/kotlin-1.3.61-blue.svg)](http://kotlinlang.org/)
[![Prime version](https://img.shields.io/github/tag/ostelco/ostelco-core.svg)](https://github.com/ostelco/ostelco-core/tags)
[![GitHub license](https://img.shields.io/github/license/ostelco/ostelco-core.svg)](https://github.com/ostelco/ostelco-core/blob/master/LICENSE)

[![Build Status](https://travis-ci.org/ostelco/ostelco-core.svg?branch=master)](https://travis-ci.org/ostelco/ostelco-core) 
[![codebeat badge](https://codebeat.co/badges/e4c26ba7-75d6-48d2-a3d0-f72988998642)](https://codebeat.co/projects/github-com-ostelco-ostelco-core-master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d15007ecfc2942f7901673177e147d09)](https://www.codacy.com/app/vihang.patil/ostelco-core?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ostelco/ostelco-core&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/d15007ecfc2942f7901673177e147d09)](https://www.codacy.com/app/vihang.patil/ostelco-core?utm_source=github.com&utm_medium=referral&utm_content=ostelco/ostelco-core&utm_campaign=Badge_Coverage)

[![CircleCI](https://circleci.com/gh/ostelco/ostelco-core/tree/develop.svg?style=svg&circle-token=f6bf824c3910dcf1551bf3b2776715dabfc7dc41)](https://circleci.com/gh/ostelco/ostelco-core/tree/develop)

# ostelco-core


`ostelco-core` is a telecom backend monorepo centered on an OCS/BSS for packet data. It covers subscriber management, online charging, bundle purchases, payments, eKYC, SIM and eSIM provisioning, support and admin workflows, analytics, and the surrounding operational tooling needed to run those services.

Most of the repository is Kotlin/JVM code organized as a Gradle multi-project build. The main application is `prime`, a modular Dropwizard service that hosts customer-facing APIs, back-office endpoints, charging logic, SIM management, notifications, and external integrations. Around `prime` sit standalone and support components such as `ocsgw` for DIAMETER Gy traffic, `auth-server`, analytics and export jobs, load and acceptance test tooling, and operational utilities.

At the storage and integration level, the repository combines Google Cloud services, Neo4j, Stripe, Firebase, and telecom-specific protocols such as DIAMETER and gRPC. The table below gives a short description of the main documentation entry points and components in this repository.

| Component | Description |
| --- | --- |
| [The big picture (diagram) of current workflow](https://github.com/ostelco/ostelco-docs/blob/master/the-current-work-flow.md) | High-level workflow diagram showing the main moving parts and message flow. |
| [General documentation](./docs/README.md) | Entry point for development, deployment, testing, logging, glossary, and architecture notes. |
| [admin-endpoint](./admin-endpoint/README.md) | Back-office API for managing offers, segments, plans, payment inspection, imports, and KYC callbacks. |
| [auth-server](./auth-server/README.md) | Standalone service that issues Firebase-backed authentication tokens for subscribers. |
| [customer-endpoint](./customer-endpoint/README.md) | Customer-facing API for profile, regions, KYC, bundles, purchases, payment sources, and SIM actions. |
| [analytics-module](./analytics-module/README.md) | Publishes analytics events and documents the Pub/Sub to Dataflow to BigQuery pipeline. |
| [diameter-stack](./diameter-stack/README.md) | Shared DIAMETER protocol library used by the telecom-facing gateway components. |
| [diameter-test](./diameter-test/README.md) | Test support library for generating DIAMETER traffic in unit and integration tests. |
| [exporter](./exporter/README.md) | Exports pseudonymised subscriber consumption and purchase data for downstream analysis. |
| [neo4j-store](./neo4j-store/README.md) | Graph-storage module and utilities for working with Neo4j-backed domain data. |
| [ocs-grpc-api](./ocs-grpc-api/README.md) | Protobuf and gRPC contract between the OCS gateway and the internal charging service. |
| [ocsgw](./ocsgw/README.md) | DIAMETER gateway that speaks Gy toward the packet gateway and forwards charging requests inward. |
| [payment-processor](./payment-processor/README.md) | Stripe integration for payment events, saved payment sources, and recurring billing workflows. |
| [prime](./prime/README.md) | Main modular Dropwizard application that hosts the customer, admin, charging, SIM, and integration modules. |
| [prime/infra](./prime/infra/README.md) | Deployment and operations notes for running Prime in Kubernetes and cloud environments. |
| [seagull](./seagull/README.md) | Dockerized Seagull traffic generator for load and protocol testing against `ocsgw`. |
| [neo4j-admin-tools](./tools/neo4j-admin-tools/README.md) | Utility tools for backing up, restoring, and exporting Neo4j data. |
