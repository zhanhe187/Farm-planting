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
- `init-daihaojie-xm.sql` — full DDL + seed data (authoritative, everything combined)
- `db-triggers.sql` — immutable event triggers (block UPDATE/DELETE on `plant_operation`)
- `db-views.sql` — indexed views
- `db-ai.sql` — AI provider, bind token, call log, and recognition log tables

Spring `sql.init.mode=never` — the app does NOT auto-create tables.

## Architecture

Single-module Spring Boot 2.7 app. Java 8 source level, runs on JDK 17.

### Layers

- `com.farm.fpms.controller` — Spring MVC controllers + Thymeleaf views. Session-based auth via `SessionInterceptor` in `WebConfig`. No Spring Security.
- `com.farm.fpms.service` — Business logic. Services use `JdbcTemplate` directly (no MyBatis-Plus mappers in practice despite the dependency).
- `com.farm.fpms.entity` — Domain objects: enums, status machine, data carriers (AiProvider, BindToken, etc.).
- `com.farm.fpms.common` — Shared utilities: `BusinessException`, `SessionUser`, `StatusLabel`.
- `com.farm.fpms.config` — Configuration classes: `WebConfig`.

### Key Libraries

- **Hutool** (`cn.hutool`) — general-purpose Java utility library
- **ZXing** (`com.google.zxing`) — QR code generation
- **EasyExcel** (`com.alibaba`) — Excel import/export
- **Thumbnailator** (`net.coobird`) — image resizing/thumbnails
- **Caffeine** — in-memory cache (available but not heavily used)

### Auth Model

No Spring Security. `WebConfig.SessionInterceptor` checks `HttpSession` for `SessionUser` and delegates route permissions to `RoleAccessPolicy`. Roles: SUPER_ADMIN, FARM_OWNER, AGRI_TECH, WAREHOUSE, FIELD_WORKER, CUSTOMER. AI provider management (`/providers`, `/admin`) is restricted to SUPER_ADMIN only. Public registration creates CUSTOMER / OWN_CUSTOMER users. Data visibility is still lightweight; customer sales are filtered by display name, while broader row-level `DataScope` enum values (ALL, OWN_PLOT, ASSIGNED_TASK, OWN_CUSTOMER) remain available for future expansion.

Public paths (no session): `/login`, `/register`, `/logout`, `/trace/**`, `/m/**`, static resources.

Default accounts (password `123456`): `admin`, `owner`, `agri`, `warehouse`, `worker`, `customer`.

### Key Domain Concepts

**Batch lifecycle** (`BatchStatusMachine`): PLANNED → SOWED → GROWING → READY_HARVEST → HARVESTING → COMPLETED. Any non-terminal state can also → ABANDONED. Transitions are enforced in code and logged to `batch_status_log`.

**Operations are append-only**: `plant_operation` rows are never updated or deleted — SQL Server triggers enforce this. Operations auto-deduct stock via `StockService`.

**Safety interval check** (`SafetyIntervalChecker`): Before harvest, validates that enough days have passed since the last pesticide application. Hard gate — harvest is blocked if the interval hasn't elapsed.

**Traceability** (`TraceService` + `TraceController`): Public unauthenticated endpoint `/trace/{code}` exposes harvest provenance. No login required.

**Sales and customer purchase module**: `sale_order` table + `SaleService` + `SaleController`. Managers use `/sales` to create/view sales; customers use `/market` to buy active crops at the current `farm_crop.sale_price_per_kg`, which writes the same sales flow and then appears in their own ledger. Revenue KPIs (total + monthly) shown on dashboard.

### AI Gateway Architecture

All AI calls go through the `AiGateway` interface (`chat`, `chatStream`, `recognize`), implemented by `OpenAiCompatibleClient`. This client supports two OpenAI API patterns:

- **`/chat/completions`** — standard chat API with `messages` array
- **`/responses`** — newer OpenAI Responses API (detected when base URL ends with `/responses`), uses `instructions` + `input` fields

Every method checks `usesResponsesApi()` and dispatches to the correct payload format.

**Provider management** (`AiProviderService`): Provider configs stored in `ai_provider` table. API keys encrypted with `AiKeyCodec` (AES-256-GCM when `FPMS_AI_MASTER_KEY` env var is set, otherwise stored as plaintext with a prefix marker). The service maintains an in-memory `AtomicReference<List<AiProvider>>` cache, invalidated on create/update/delete.

**API key masking**: `AiKeyCodec.mask()` shows `sk-********abcd` format. On edit, leaving the API key field empty preserves the existing key — only a non-empty value overwrites.

**Provider types**: Each provider has a `scene` (CHAT or VISION). Vision providers must support image input — DeepSeek is explicitly rejected for vision since it doesn't.

**Streaming** (`/ai/stream`): Uses Spring `WebClient` for SSE token-by-token streaming from OpenAI-compatible providers. Frontend consumes via `EventSource` API. Non-streaming fallback when using Responses API.

**Sensitive data masking**: The `fpms.ai.chat.sensitive-mask` config lists regex patterns (phone numbers, ID numbers) that get masked in AI prompts.

### Mobile & Vision

**Device binding** (`BindTokenService` + `MobileController`):
- `GET /mobile` — PC admin page, generates a one-time QR code containing a bind token (5-min TTL by default)
- `GET /m/bind` — mobile device scans QR, submits device info, binds to the PC session
- `POST /m/bind` — consumes the token (one-time use), creates `account_device` row

**LAN IP detection** (`LanAccessService`): When serving QR codes from `localhost`, automatically detects the machine's LAN IP so mobile devices can reach the bind URL. Prefers Wi-Fi > Ethernet, skips VM/docker/virtual interfaces. Override with `FPMS_MOBILE_BASE_URL` env var or `fpms.ai.bind.qr-base-url` config.

**Photo recognition** (`MobileVisionService` + `POST /m/recognize`): Mobile device uploads a photo → converted to base64 data URL → sent to VISION-scoped AI provider → returns crop name, latin name, usage, cultivation info, soil, common pests, confidence. Results logged to `ai_recognize_log` with image SHA-256 hash. Image limits: max 10MB, JPG/PNG/WebP only.

### Chinese Localization

`StatusLabel` utility class provides `batchStatus()`, `operationType()`, `role()` methods for code→Chinese mapping. Injected into all Thymeleaf models via `WebConfig.postHandle`. Templates use `${label.batchStatus(...)}` etc.

### Frontend

Server-rendered Thymeleaf templates in `src/main/resources/templates/`. Single CSS file `static/css/app.css` (dark "field command" theme). No JS framework — vanilla JS for dashboard charts and interactions.

## Configuration

Key `fpms.*` properties in `application.yml`:

| Property | Default | Purpose |
|---|---|---|
| `fpms.ai.master-key` | (env `FPMS_AI_MASTER_KEY`) | AES-256 key for encrypting stored AI API keys |
| `fpms.ai.bind.qr-base-url` | (env `FPMS_MOBILE_BASE_URL`) | Override auto-detected LAN URL for QR codes |
| `fpms.ai.bind.token-ttl-seconds` | 300 | Bind token expiration |
| `fpms.ai.chat.rate-limit-per-min` | 10 | Chat rate limit |
| `fpms.ai.chat.sse-timeout-ms` | 120000 | SSE stream timeout |
| `fpms.ai.chat.sensitive-mask` | phone/ID regexes | Patterns masked in AI prompts |
| `fpms.upload.path` | `./static/upload/` | File upload directory |
| `fpms.upload.allow-ext` | jpg,jpeg,png,webp,pdf | Allowed upload extensions |
| `fpms.trace.base-url` | `http://localhost:8080/trace/` | Base URL for trace links |
| `fpms.trace.rate-limit-per-min` | 30 | Trace endpoint rate limit |

## Testing

All tests run against a real SQL Server database — there is no H2 or in-memory database for tests. Tests use `JdbcTemplate` for setup/assertions directly. Test classes are in `src/test/java/com/farm/fpms/` mirroring the main source structure.

### Test Categories

- **Domain tests**: `BatchStatusMachineTest` (state transitions), `SafetyIntervalCheckerTest` (harvest safety gates)
- **Service tests**: `StockServiceTest`, `OperationServiceTest` (stock deduction + append-only), `BindTokenServiceTest` (token lifecycle), `AiProviderServiceTest` (CRUD + caching), `OpenAiCompatibleClientTest` (API format dispatch), `MobileVisionUploadServiceTest` (photo validation), `LanAccessServiceTest` (IP detection), `PromptBuilderTest` (sensitive data masking), `PasswordServiceTest`, `UserRegistrationServiceTest`
- **Config tests**: `SqlServerInitScriptTest` (validates init SQL can execute), `SqlServerConfigurationTest`
- **Web tests**: `AuthControllerRegistrationTest`

## Documentation

The repository intentionally keeps documentation lean:
- `README.md` — build, run, accounts, and feature overview
- `doc/系统使用说明文档.md` — business flow, role permissions, and operating guide
