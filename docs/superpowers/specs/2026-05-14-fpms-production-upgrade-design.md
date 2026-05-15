# FPMS Production Upgrade Design

## Goal

Upgrade the existing FPMS Java project from a runnable classroom-style MVP into a small deployable project. The implementation remains intentionally lightweight, but the code and frontend must no longer present itself as a demo. Operational notes, default accounts, local-mode limitations, and extension guidance belong in documentation.

## Approach

Use an incremental hardening path:

1. Remove product-facing demo language from Java code, HTML templates, static UI text, and seed data.
2. Keep local bootstrap instructions in README and add a deployment-oriented note document.
3. Keep authentication lightweight with direct password comparison for local deployment.
4. Use the current session user as the operator for status transitions and harvest records.
5. Keep H2 as the default quick-start database, while preserving SQL Server scripts for landing on the target database.

## Scope

In scope:

- Plain password verification service and tests.
- Hashed seed passwords in `data.sql`.
- Login page with production-style wording, no displayed default password.
- AI, mobile, provider, and dashboard text changed from demo descriptions to feature descriptions.
- Existing business flows continue to work after the upgrade.
- Documentation explains local bootstrap accounts and which integrations are local fallback implementations.

Out of scope:

- Introducing full Sa-Token or Spring Security filter-chain migration.
- Real external AI provider calls.
- Full database migration framework such as Flyway.
- Multi-tenant SaaS isolation.

## Acceptance Criteria

- `rg -n "演示|MVP|模拟|默认密码|demo" src/main/java src/main/resources/templates src/main/resources/static src/main/resources/data.sql pom.xml` returns no product-facing occurrences.
- `mvn test` passes.
- Login with `owner / 123456` works.
- Main pages return HTTP 200 after login.
- README or docs contain local bootstrap account information.

## Self Review

- The scope directly addresses the user's request to avoid putting demo explanations in code and frontend.
- The design preserves the planning document's architecture and does not add unnecessary framework complexity.
- Known local-mode behavior is documented rather than displayed in the application UI.
