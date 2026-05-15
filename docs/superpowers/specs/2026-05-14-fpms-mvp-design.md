# FPMS MVP Design

## Goal

Build a runnable Java web application for the farm planting management system described in `D:\javacode\农场种植管理系统-规划文档.md`. The MVP must demonstrate the core planting lifecycle, inventory linkage, safety interval compliance, traceability, dashboard visualization, AI assistant entry, and mobile crop recognition entry.

## Scope

This implementation follows the planning document's MUST scope, compressed into a local demo-ready system:

- Land plot, crop, material, batch, operation, inventory, harvest, sales-like traceability records.
- Batch lifecycle state machine: `PLANNED -> SOWED -> GROWING -> READY_HARVEST -> HARVESTING -> COMPLETED`, plus `ABANDONED`.
- Append-only operation workflow at service level and SQL Server trigger script level.
- Operation and material consumption in one transaction.
- Pesticide safety interval precheck and hard enforcement before harvest.
- Trace code generation and public trace page with desensitized operation timeline.
- Dashboard with KPIs, ECharts charts, and Canvas plot layout.
- Session-based demo RBAC with roles from the planning document.
- AI assistant UI with backend SSE-compatible endpoint and deterministic local fallback responses.
- AI provider configuration page with masked keys and a test action stub.
- Mobile H5 bind/recognize flow with one-time token behavior and deterministic crop-recognition fallback.

## Architecture

The project is created under `D:\javacode\fpms-mvp` as a Spring Boot 2.7 Maven application. The app uses Thymeleaf for server-rendered pages, H2 for default local execution, and SQL Server scripts for production-shaped schema, immutable operation triggers, indexed views, and AI tables. The backend keeps domain behavior in focused services so the core rules are testable without the web layer.

The MVP uses `JdbcTemplate` repositories for concise data access while the `pom.xml` includes the planning document's key dependencies, including MyBatis-Plus, SQL Server driver, Caffeine, zxing, WebFlux, and validation. This keeps the application runnable quickly and leaves a clear migration path to MyBatis-Plus mappers.

## UI Direction

Use the local frontend-design skill direction chosen by the user: "田间指挥台". The interface is a practical farm command center: earthy off-white background, deep field green navigation, harvest amber warning accents, dense scan-friendly tables, compact KPI panels, Canvas plot visualization, and a floating AI assistant button. It avoids a marketing landing page; the first authenticated page is the operating dashboard.

## Data Model

Core H2 tables:

- `sys_user`: demo users, roles, and data scope.
- `farm_plot`: plot metadata plus `layout_x`, `layout_y`, `width`, and `height`.
- `farm_crop`: crop catalog and shortest growth days.
- `farm_material`: seed, fertilizer, and pesticide catalog with safe interval days.
- `stock_inventory`: current material stock with safety stock and version column.
- `plant_batch`: planting batch lifecycle, plot, crop, planned area, dates, and trace code.
- `batch_status_log`: transition audit log.
- `plant_operation`: append-only operation event stream.
- `plant_operation_material`: operation material consumption details.
- `stock_out_order`: generated stock out records linked to operations.
- `harvest_record`: harvest quantity and trace code.
- `trace_public_field`: public trace field whitelist.
- `ai_provider`, `ai_call_log`, `ai_bind_token`, `ai_recognize_log`: AI and mobile demo support.

## Core Behavior

`BatchStatusMachine` owns legal status transitions and exposes a pure validation method. `BatchService` uses it to update status and write `batch_status_log`.

`OperationService` inserts operation events and material details in one transaction. If materials are supplied, it calls `StockService.consume`, which rejects insufficient stock and writes stock-out orders. Existing operation rows are not updated by application code.

`SafetyIntervalChecker` calculates the latest forbidden harvest date from pesticide operations and material safe intervals. `HarvestService` calls it for both precheck and hard enforcement, then advances the batch through `HARVESTING` and `COMPLETED`, creates a harvest record, and writes a trace code if missing.

`TraceService` returns a dedicated public view object instead of exposing business entities. It only includes crop, plot, harvest date, safe interval result, and desensitized operation type/date/worker timeline.

`AiAssistantService` builds responses from local business context. It has an SSE-ready controller path so a real OpenAI-compatible provider can be connected later without changing the UI contract.

`MobileVisionService` validates one-time bind tokens and returns structured crop-recognition demo results matching the planning document's JSON shape.

## Testing

The test suite covers the highest-risk domain rules:

- Legal and illegal batch status transitions.
- Safety interval precheck across no pesticide, safe pesticide, and unsafe pesticide cases.
- Inventory consumption and insufficient stock rejection.
- Harvest enforcement blocked before safe interval and allowed after safe interval.
- Prompt masking for AI assistant context.
- One-time mobile bind token behavior.

## Out Of Scope For This MVP

These are represented by scripts, stubs, or extension points, not full production implementations:

- Real Sa-Token integration and MyBatis-Plus data permission interceptor.
- Real external AI provider calls and AES-GCM key storage.
- Real QR PNG generation for every harvest; trace URLs are generated and displayed.
- Full EasyExcel import/export.
- Full SQL Server runtime validation on this machine.
- Multi-tenant SaaS mode and IoT integrations.

## Self Review

- No placeholder requirements remain in this MVP design.
- The architecture matches the chosen A implementation route and preserves the planning document's core features.
- Full production-only requirements are explicitly scoped as scripts or extension points.
- The UI direction is the user-approved "田间指挥台" direction.
