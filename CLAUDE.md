# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Expected behavior

Challenge the developer's way of thinking, don't validate it. When a plan or technical decision is proposed:

- Point out flaws, edge cases and blind spots
- Disagree when the logic is weak or there are unsupported assumptions
- Say directly when something is a bad idea, instead of making it work anyway

No encouragement or positivity needed. Critical thinking and direct corrections are needed.

## What this project is

Chepa Motos is a full-stack invoice and commission management system for a motorcycle repair shop. It consists of a Spring Boot REST API (`chepa-api`) and a .NET MAUI desktop/mobile app (`chepa-desktop`), backed by PostgreSQL and Metabase for analytics. Everything runs via Docker Compose for local development.

---

## Commands

### Backend (`chepa-api`) — Gradle

```bash
./gradlew bootRun           # Run Spring Boot locally (port 8080)
./gradlew test              # Integration + adapter tests (requires running DB)
./gradlew domainTest        # Domain unit tests only (no Spring, no DB)
./gradlew ciDomainCoverage  # Domain tests + JaCoCo coverage (enforces 70% minimum)
./gradlew jacocoTestReport  # Generate HTML coverage report
./gradlew build             # Full build including all tests
```

### Frontend (`chepa-desktop`) — .NET MAUI

```bash
dotnet build    # Build the project
dotnet run      # Run on Windows
```

### Infrastructure

```bash
docker compose up -d   # Start PostgreSQL (port 5432) + Metabase (port 3000)
```

Copy `.env.example` to `.env` and fill in `DB_USER` and `DB_PASSWORD` before running Docker Compose.

---

## Architecture

### Monorepo layout

```
chepa-motos/
├── chepa-api/          ← Spring Boot REST API (Java 25, Spring Boot 4.0.3)
├── chepa-desktop/      ← .NET MAUI app (net10.0-windows + net10.0-android)
├── init/               ← Database init SQL (schema + seed)
├── docker-compose.yml  ← PostgreSQL 18 + Metabase OSS
└── .env.example
```

### Backend — Clean/Hexagonal Architecture

The backend strictly separates three layers:

- **`domain/`** — Pure business logic: use cases, domain models, port interfaces, domain exceptions. No Spring, no JPA. This is the only layer with unit tests (run via `./gradlew domainTest`).
- **`adapter/`** — REST controllers, request/response DTOs, Spring MVC config, exception handlers.
- **`infrastructure/`** — JPA entities, repository implementations, entity-to-domain mappers.

Never put business logic outside `domain/`. Controllers delegate directly to use cases.

### Frontend — MVVM + Service Layer

The frontend follows a strict service-layer pattern:

- **`Services/`** — All HTTP calls. One service class per backend resource (`MechanicsService`, `InvoicesService`, etc.).
- **`ViewModels/`** — Call service methods, hold observable state. Never construct `HttpClient` or call URLs directly.
- **`Views/`** — XAML pages only. No data access.
- **`Models/`** — DTOs with `[JsonPropertyName]` for snake_case mapping. Check here before creating a new model.
- **`Config/AppConfig.cs`** — Base URL. Always use `AppConfig.BaseUrl` in services, never hardcode.

---

## API contract

**Base URL:** `http://localhost:8080/api`

Every successful response is wrapped: `{ "data": ..., "timestamp": "..." }`. Services must unwrap `data` before returning to ViewModels — never return the envelope.

Every error response: `{ "code": "ERROR_CODE", "message": "...", "timestamp": "..." }`

**Important error handling rules:**
- `VEHICLE_NOT_FOUND` (404) is NOT an error — it means the plate is new, keep the model field editable.
- Empty suggestions array from `/invoice-items/suggestions` is NOT an error — handle silently.

**Key conventions:**
- Dates: `YYYY-MM-DD`, Timestamps: `YYYY-MM-DDTHH:mm:ss`
- Currency: `decimal` with 2 decimal places (e.g. `209100.00`). Display as `$209.100` (Colombian peso). Never send formatted strings to the backend.
- Plate: always `.ToUpperInvariant().Trim()` before sending.
- Autocomplete (`/invoice-items/suggestions`): debounce 300ms, require ≥2 chars, only call for SERVICE invoices.

**Resources:** `/mechanics`, `/vehicles`, `/invoices` (with `/service` and `/delivery` sub-paths), `/invoice-items/suggestions`, `/liquidations`

---

## Business rules

- **SERVICE invoices:** require a mechanic, vehicle, at least one item, optional labor amount. Backend calculates subtotals and totals.
- **DELIVERY invoices:** buyer name + items only. No mechanic, no vehicle, no labor.
- **Liquidations:** only SERVICE invoices contribute to mechanic commissions (70% mechanic / 30% shop). DELIVERY invoices are fully excluded.
- The entire invoice lives in memory until the user confirms — `POST /invoices/service` or `POST /invoices/delivery` is called exactly once per invoice at confirmation.
- Backend normalizes plates and text — but frontend normalizes too before sending.

---

## Branch naming

- `feature/api/name` — backend features
- `feature/desktop/name` — frontend features
- `fix/api/name` — backend fixes
- `fix/desktop/name` — frontend fixes
- `main` is protected; changes go through pull requests only.
