---
name: spring-adapter-testing
description: Use when implementing or testing Spring MVC JSON endpoints, SQL persistence adapters, external provider clients, or their boundary mappings in this repository.
---

# Spring Adapter Testing

Read `AGENTS.md` first; it defines the repository's architecture and test rules.

- For an HTTP endpoint, test request validation, JSON mapping, successful response, and relevant failure-to-status mappings with a focused web test. Keep controller behavior to translation and delegation to the use case.
- For a SQL adapter, test row-to-domain mapping, persistence of important invariants, and query/transaction behavior against a real database when SQL-specific behavior matters. Keep SQL and row models out of the domain; do not choose a database or migration library without a concrete need.
- For a provider adapter, test translation of provider DTOs and expected error handling using a fake/stub HTTP boundary. Do not call live providers in automated tests.
- Prefer fast domain/application unit tests for business behavior; avoid booting all of Spring for a rule that has no framework dependency. Run `./gradlew test` after changes.
