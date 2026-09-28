---
name: kotlin-ddd-hexagonal
description: Use when implementing Kotlin Spring Boot domain models, use cases, ports, adapters, or domain events using DDD and hexagonal architecture in this repository.
---

# Kotlin DDD And Hexagonal Architecture

Read `AGENTS.md` first; it defines the repository's architecture rules.

For each use case:

1. Name the business outcome and identify the domain object that owns each invariant or decision. Put behavior there; use a domain service only when no object naturally owns it.
2. Write an application service that describes the steps of the use case and declares outbound ports in `application` for capabilities it needs. Keep Spring, SQL, HTTP, and provider types out of `domain`.
3. Implement inbound HTTP mapping in `presentation` and outbound integrations in `infrastructure`. Translate provider models at the adapter boundary.
4. If the use case produces domain events, let the domain record immutable facts and let the application coordinate in-process dispatch after successful persistence. Ask before introducing cross-system or reliably delivered events.
5. Test invariants and orchestration without booting Spring, and add focused adapter tests for changed boundaries.

Start with the existing single bounded context and four layers; propose a context split only when concrete language or ownership boundaries warrant it.
