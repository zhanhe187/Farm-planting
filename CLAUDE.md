# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build (skip tests)
mvn package -DskipTests

# Run application (port 8080)
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=BatchStatusMachineTest

# Run a single test method
mvn test -Dtest=StockServiceTest#consumeReducesAvailableStock
```

## Database

SQL Server on localhost:1433, database `daihaojie_xm`, user `sa`/`123456`.

First-time init:
```powershell
sqlcmd -S localhost -U sa -P 123456 -C -b -f 65001 -i src\main\resources\db\init-daihaojie-xm.sql
```

Schema lives in `src/main/resources/db/`:
- `init-daihaojie-xm.sql` — full DDL + seed data (authoritative)
- `db-triggers.sql` — immutable event triggers (block UPDATE/DELETE on operations)
- `db-views.sql` — indexed views
- `db-ai.sql` — AI provider/log tables

Spring `sql.init.mode=never` — the app does NOT auto-create tables.

## Architecture

Single-module Spring Boot 2.7 app. Java 8 source level, runs on JDK 17.

### Layers

- `com.farm.fpms.web` — Spring MVC controllers + Thymeleaf views. Session-based auth via `SessionInterceptor` in `WebConfig`. No Spring Security.
- `com.farm.fpms.service` — Business logic. Services use `JdbcTemplate` directly (no MyBatis-Plus mappers in practice despite the dependency).
- `com.farm.fpms.domain` — Enums and domain rules (status machine, data scope, material categories).

### Key Domain Concepts

**Batch lifecycle** (`BatchStatusMachine`): PLANNED → SOWED → GROWING → READY_HARVEST → HARVESTING → COMPLETED. Any non-terminal state can also → ABANDONED. Transitions are enforced in code and logged to `batch_status_log`.

**Operations are append-only**: `plant_operation` rows are never updated or deleted — SQL Server triggers enforce this. Operations auto-deduct stock via `StockService`.

**Safety interval check** (`SafetyIntervalChecker`): Before harvest, validates that enough days have passed since the last pesticide application. This is a hard gate on the harvest flow.

**Traceability** (`TraceService` + `TraceController`): Public unauthenticated endpoint `/trace/{code}` exposes harvest provenance. No login required.

**AI assistant** (`AiAssistantService`): Calls external LLM via OpenAI-compatible API. Provider config stored in DB, key encrypted with `AiKeyCodec`. SSE streaming supported.

**Mobile bind** (`BindTokenService`): One-time QR tokens (5-min TTL) for field workers to bind devices without passwords.

### Auth Model

No Spring Security. `WebConfig.SessionInterceptor` checks `HttpSession` for `SessionUser`. Roles: SUPER_ADMIN, FARM_OWNER, AGRI_TECH, WAREHOUSE, FIELD_WORKER, CUSTOMER. Admin-only paths (`/providers`, `/admin`) are restricted to SUPER_ADMIN and FARM_OWNER via `SessionUser.isAdmin()`. Data visibility controlled by `DataScope` enum (ALL, OWN_PLOT, ASSIGNED_TASK, OWN_CUSTOMER).

Public paths (no session): `/login`, `/logout`, `/trace/**`, `/m/**`, static resources.

### Chinese Localization

`StatusLabel` utility class provides `batchStatus()`, `operationType()`, `role()` methods for code→Chinese mapping. Injected into all Thymeleaf models via `WebConfig.postHandle`. Templates use `${label.batchStatus(...)}` etc.

### AI Streaming

`/ai/stream` endpoint uses `SseEmitter` + `WebClient` for true token-by-token SSE streaming from OpenAI-compatible providers. Frontend uses `EventSource` API for real-time rendering.

### Sales Module

`sale_order` table + `SaleService` + `SaleController` at `/sales`. Revenue KPIs (total + monthly) shown on dashboard.

### Frontend

Server-rendered Thymeleaf templates in `src/main/resources/templates/`. Single CSS file `static/css/app.css` (dark "field command" theme). No JS framework — vanilla JS for dashboard charts and interactions.
