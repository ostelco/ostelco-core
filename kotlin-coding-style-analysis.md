# Kotlin Coding Style Analysis

This note looks at one specific question in `ostelco-core`: the coexistence of two Kotlin styles that were never fully reconciled.

One style is conventional object-oriented service code, translated into Kotlin: modules, resources, DAOs, implementations, service locators, singletons, and framework-oriented classes.

The other style is a more functional, sometimes distinctly Haskell-influenced Kotlin: explicit `Either`-based error flow, dense use of Arrow combinators, data classes as primary domain carriers, top-level functions, and even an internal graph DSL based on `infix` expressions.

The short conclusion is that both styles are real, both are intentional, and they are in visible tension. The codebase is not stylistically random, but it is split-brained.

## The Two Styles

### 1. OO-and-framework Kotlin

This style is most visible at application boundaries:

- [`prime/src/main/kotlin/org/ostelco/prime/PrimeApplication.kt`](./prime/src/main/kotlin/org/ostelco/prime/PrimeApplication.kt)
- [`prime-modules/src/main/kotlin/org/ostelco/prime/module/PrimeModule.kt`](./prime-modules/src/main/kotlin/org/ostelco/prime/module/PrimeModule.kt)
- [`prime-modules/src/main/kotlin/org/ostelco/prime/module/ResourceRegistry.kt`](./prime-modules/src/main/kotlin/org/ostelco/prime/module/ResourceRegistry.kt)
- [`customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/resources/CustomerResource.kt`](./customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/resources/CustomerResource.kt)
- [`customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/resources/KycResources.kt`](./customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/resources/KycResources.kt)

Its traits are familiar:

- framework entry points are classes
- endpoints are Jersey resources
- subsystems are exposed as interfaces plus `*Impl` classes
- service discovery uses `ServiceLoader`
- inheritance and framework lifecycle hooks are normal tools
- side effects are performed directly in the same layer that owns request handling

This style reads like a well-structured Dropwizard service written by people with Java and backend service backgrounds who were happy to use Kotlin as a better Java.

### 2. Functional or Haskell-oriented Kotlin

This style is strongest in storage, domain flow, and error handling:

- [`customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/store/SubscriberDAO.kt`](./customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/store/SubscriberDAO.kt)
- [`customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/store/SubscriberDAOImpl.kt`](./customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/store/SubscriberDAOImpl.kt)
- [`payment-processor/src/main/kotlin/org/ostelco/prime/paymentprocessor/StripePaymentProcessor.kt`](./payment-processor/src/main/kotlin/org/ostelco/prime/paymentprocessor/StripePaymentProcessor.kt)
- [`payment-processor/src/main/kotlin/org/ostelco/prime/paymentprocessor/StripeUtils.kt`](./payment-processor/src/main/kotlin/org/ostelco/prime/paymentprocessor/StripeUtils.kt)
- [`data-store/src/main/kotlin/org/ostelco/prime/store/datastore/Schema.kt`](./data-store/src/main/kotlin/org/ostelco/prime/store/datastore/Schema.kt)
- [`neo4j-store/src/main/kotlin/org/ostelco/prime/dsl/Syntax.kt`](./neo4j-store/src/main/kotlin/org/ostelco/prime/dsl/Syntax.kt)
- [`neo4j-store/src/main/kotlin/org/ostelco/prime/dsl/Transactions.kt`](./neo4j-store/src/main/kotlin/org/ostelco/prime/dsl/Transactions.kt)

Its traits are equally clear:

- `Either<Error, Value>` is the default way to represent fallible work
- Arrow operators such as `mapLeft`, `flatMap`, `bimap`, `fold`, `left`, and `right` are common
- domain entities are mostly `data class` values
- error mapping is explicit rather than exception-driven
- internal DSLs use top-level and `infix` functions to encode domain relations
- control flow is often expressed as composition rather than imperative sequencing

This is not just "using some Kotlin features". In places such as the Neo4j DSL it is deliberately pushing Kotlin toward a small, typed language for expressing domain facts.

## Where the Tension Shows Up

### Error handling is split across two philosophies

The functional side wants explicit error values. The OO side still uses `try/catch`, logging, and framework `Response` building. A good example is [`SubscriberDAOImpl`](./customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/store/SubscriberDAOImpl.kt), which exposes `Either<ApiError, T>` almost everywhere, but still wraps many methods in broad `try/catch` blocks and converts unexpected failures back into left values.

That means the code often pays for both systems:

- explicit error algebra
- exception-based escape hatches

This is workable, but it is stylistically noisy.

### Dependency flow is not uniformly explicit

The more functional parts read as if they want explicit composition, but the runtime model often falls back to global lookup:

- `getResource<T>()` uses `ServiceLoader`
- several modules use singleton `object`s
- resource classes and service implementations discover collaborators indirectly

That weakens one of the main benefits of the functional style: transparent dependencies.

### OO shell and functional core were never named as such

The code often behaves as if the intended architecture was "functional core, imperative shell", but that design rule does not seem to have been written down. As a result, different modules drift to different balances:

- resource classes are fairly conventional
- DAO and service layers often return `Either`
- storage and graph code become very DSL-heavy
- framework wiring remains global and imperative

The style boundary is therefore real, but not clean.

### The functional style is tied to an older Arrow era

The repo uses Arrow in a way that was idiomatic for the Kotlin 1.3 timeframe. In 2026 that style looks historically specific:

- lots of `Either`
- `Try`
- manual `.left()` and `.right()`
- combinator-heavy chains

The underlying intent still makes sense, but the exact idiom now feels dated.

### Inheritance and DSLs pull in opposite directions

The resource layer sometimes uses inheritance and framework composition in a classical OO way, as in [`KycResources.kt`](./customer-endpoint/src/main/kotlin/org/ostelco/prime/customer/endpoint/resources/KycResources.kt). The Neo4j layer goes the other way entirely and builds a domain DSL around `infix` expressions such as `Customer withId "x"` and relation builders in [`Syntax.kt`](./neo4j-store/src/main/kotlin/org/ostelco/prime/dsl/Syntax.kt).

Those are very different ideas of what "good Kotlin" looks like.

## The Important Thing: The Split Is Not Random

The stylistic split more or less follows subsystem boundaries:

- HTTP and framework entry points lean OO
- domain and storage abstractions lean functional
- graph modeling leans strongly DSL-oriented
- telecom gateway code is mixed, often pragmatic rather than stylistically pure

That is actually a useful observation. It means the repo can be read as an attempted architecture, not just a coding-style argument.

## My Assessment

The functional style is the more distinctive and interesting one. It is where the codebase feels most like Kotlin rather than Java-with-better-syntax. It also aligns well with a domain that benefits from explicit state, explicit errors, and careful composition.

The OO style is the more operationally legible one. It makes the HTTP layer, modules, and framework integration easier for a wider range of backend developers to navigate.

The problem is not that both styles exist. The problem is that the codebase never clearly decided where each style should dominate.

## What I Would Preserve If The Repo Were Revived

If this codebase were revived, I would preserve the following ideas:

- domain entities as value types
- explicit error types instead of letting business failures vanish into exceptions
- a mostly functional core for business rules and storage operations
- a conventional OO shell for HTTP, DI, lifecycle, and vendor SDK integration

In other words, I would keep the coexistence, but make it explicit.

## What I Would Change

### 1. Name the architecture

Write down a simple rule:

"Use a functional core and an imperative shell."

That one sentence would make a surprising amount of the repo easier to reason about.

### 2. Replace service-locator style wiring

If the functional core is meant to be testable and explicit, global lookup through `ServiceLoader` and singleton registries should gradually be replaced with constructor injection or clearly scoped module composition.

### 3. Reduce inheritance in the HTTP layer

The KYC resource hierarchy is understandable, but it already contains a comment warning that inheritance may stop scaling. A revived codebase should prefer composition and strategy objects over deep resource inheritance.

### 4. Modernize the functional style without flattening it

If the repo were upgraded to modern Kotlin, I would not throw away the FP instincts. I would simplify them:

- keep explicit result types
- keep sealed errors
- keep value-oriented domain modeling
- reduce older Arrow-specific ceremony where plain Kotlin now does the job cleanly

### 5. Add a written style guide

This repo is large enough that style should be documented as architecture, not treated as individual taste.

A short guide should answer:

- where `Either`-style results belong
- where exceptions are acceptable
- where mutable state is acceptable
- whether singleton objects are allowed
- whether DSL-style APIs are encouraged or exceptional

## Bottom Line

The codebase reflects two Kotlin identities:

- Kotlin as a cleaner Java for service code
- Kotlin as a language for typed functional composition

Neither side "won". The result is sometimes awkward, but it is also one of the more interesting things about the repository. If the project were ever revived, the right move would not be to erase one style in favor of the other. It would be to declare a boundary between them and make that boundary part of the design.

## Reference

For the language-level backdrop, Kotlin itself explicitly positions the language as supporting both object-oriented and functional styles:

- [Kotlin FAQ: "Is Kotlin an object-oriented language or a functional one?"](https://kotlinlang.org/docs/faq.html)
