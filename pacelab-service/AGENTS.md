# Pacelab Service Coding Guidelines

This is a Kotlin Spring Boot service for workout data, analysis, and eventually AI-assisted training conversations. Build only the use cases needed now; do not scaffold future features before they have concrete requirements. The HTTP interface is a JSON REST-like API. Authentication and the persistence technology are not yet decided.

## Architecture

- Start with one application-wide bounded context. Under `src/main/kotlin/no/sondre/pacelabservice`, organize new code by `domain`, `application`, `infrastructure`, and `presentation`. Do not create empty packages just to fill out the structure.
- `domain` contains pure Kotlin business concepts, invariants, behavior, and domain events. It depends on no other application layer, Spring, provider APIs, or persistence framework.
- `application` contains use cases and the outbound ports they need. Services orchestrate domain objects, transactions, and ports; they should read like a description of the use case. Put complex business decisions in domain objects or, when no single object owns them, domain services rather than in orchestration code.
- `presentation` is the inbound HTTP adapter: controllers, request/response models, input parsing, and mapping application outcomes to HTTP. Controllers call application use cases, not infrastructure adapters. Do not expose domain objects or provider DTOs as the HTTP contract.
- `infrastructure` holds outbound adapters, including provider clients and persistence implementations, plus Spring wiring. It implements application-defined ports. Provider-specific DTOs, OAuth/API details, and translation into application/domain concepts stay here; they must not leak into the domain.
- Dependencies point inward: presentation -> application -> domain. Infrastructure may depend on application and domain to implement ports; the application must not depend on infrastructure or presentation. Wire adapters to ports at the composition boundary.
- Treat external systems, such as Strava, as integrations rather than as domain models. Keep integration-specific code separate inside infrastructure. Revisit the single-context and package layout when distinct subdomains develop separate language, lifecycle, ownership, or enough code that layer packages become hard to navigate; then split by context without breaking dependency direction.

## Domain And Data

- Model behavior and enforce invariants in domain objects. Prefer meaningful methods and value objects to public mutable state and large conditional application services. Keep aggregate changes consistent through their owning aggregate; avoid abstraction or shared kernels without a concrete need.
- Define domain events as immutable facts about completed business changes in the domain. For now, application use cases coordinate in-process dispatch after the relevant state change is successfully persisted; do not assume this provides reliable delivery or atomicity with external work. Ask before adding an outbox or other reliable delivery mechanism when an event crosses a system boundary or lost delivery could leave the application in a bad or inconsistent state.
- Keep persistence concerns out of domain objects. Prefer direct SQL and small, explicit mappings in infrastructure when persistence is introduced; choose the database, SQL library, and migration tool with the first concrete persistence use case. Avoid introducing JPA entities that mostly duplicate domain models. Reconsider JPA later if its benefits for actual use cases outweigh mapping and framework coupling costs, while preserving a pure domain.

## API And Tests

- Expose use cases through clear JSON HTTP contracts. Validate and translate input at the presentation boundary and map failures to appropriate HTTP responses; do not leak provider errors or internal exceptions as API contracts. Do not assume an authentication scheme until one is chosen.
- Test domain rules and application orchestration with fast unit tests. For changes to an HTTP, SQL, or provider boundary, add focused tests for that adapter and its mapping/error behavior. Use broader Spring/infrastructure tests where they verify behavior that focused tests cannot.
- Run `./gradlew test` for changes that can affect the build or behavior. Keep tests and production code in their corresponding packages.

## Maintaining This Guidance

- Repository-local skills live in `.opencode/skills/`: use `kotlin-ddd-hexagonal` for domain/use-case architecture work and `spring-adapter-testing` for boundary and testing work. `AGENTS.md` is the source of truth if guidance overlaps.
- When a new recurring decision, constraint, or pitfall emerges, consider updating `AGENTS.md` for durable project-wide rules or the relevant skill for task-specific procedure. Keep both concise and revise existing guidance rather than adding duplicate or speculative policy.
