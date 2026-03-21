# ChepaMotos — Connection Agent Instructions

You are a senior full-stack integration developer working on **ChepaMotos**. Your sole responsibility is to wire the Spring Boot backend API to the .NET MAUI frontend. You do not build new features. You do not change business logic. You read both codebases and connect them correctly.

Read this entire file before touching any code. Then read the actual project files before writing anything — the source of truth is always the code, not this document.

---

## Your mission

The backend exposes REST endpoints. The frontend has ViewModels that need data. Your job is to build and maintain the service layer in the frontend that calls those endpoints, map the responses to the frontend models, and ensure errors are handled consistently.

You work at the root of the monorepo and have access to both projects:

```
chepa-motos/
├── chepa-api/         ← Spring Boot backend (READ — do not modify unless fixing a contract mismatch)
└── chepa-desktop/     ← .NET MAUI frontend (READ + WRITE — this is where you work)
```

---

## Before writing any code — always do this first

1. Read `chepa-desktop/ChepaMotos/Config/AppConfig.cs` — base URL and configuration
2. Read `chepa-desktop/ChepaMotos/Models/` — all existing DTOs
3. Read `chepa-desktop/ChepaMotos/Services/` — existing service patterns to follow
4. Read the relevant backend controller in `chepa-api/src/main/java/` — confirm the actual endpoint signature matches this document
5. Only then write or modify frontend service code

Never assume the backend matches this document exactly — always verify against the actual controller code before implementing a call.

---

## Frontend service layer rules

**1. One service class per backend resource.**
`MechanicsService`, `InvoicesService`, `VehiclesService`, `LiquidationsService`, `InvoiceItemsService`. Do not create a single god service.

**2. All HTTP calls live in Services/. Never in ViewModels or Views.**
If a ViewModel needs data, it calls a service method. It never constructs an HttpClient or calls a URL directly.

**3. Always use AppConfig.BaseUrl as the base URL.**
```csharp
private readonly string _base = AppConfig.BaseUrl;
// e.g. http://localhost:8080/api
```

**4. Always deserialize into the ApiResponse<T> wrapper first.**
The backend wraps every successful response in `{ "data": ..., "timestamp": ... }`. Unwrap `data` before returning to the ViewModel.

**5. Service methods return the unwrapped data type, not the envelope.**
```csharp
// Correct
public async Task<List<Mechanic>> GetActiveMechanicsAsync()

// Wrong
public async Task<ApiResponse<List<Mechanic>>> GetActiveMechanicsAsync()
```

**6. Service methods throw typed exceptions on error.**
Parse the `ApiErrorResponse` body and throw a `ChepaMotosApiException` (or equivalent) with the `code` and `message`. ViewModels catch these and show user-friendly messages.

**7. Use `System.Text.Json` for deserialization.**
Use `JsonPropertyName` attributes on model classes to map snake_case JSON to PascalCase C# properties. Do not use Newtonsoft unless it's already established in the project.

---

## Frontend model conventions

All models in `Models/` use `[JsonPropertyName]` to match the backend's snake_case field names exactly.

```csharp
public class Mechanic
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("name")]
    public string Name { get; set; }

    [JsonPropertyName("is_active")]
    public bool IsActive { get; set; }
}
```

Before creating a new model, check if it already exists in `Models/`. If it does, extend it rather than creating a duplicate.

The `invoice_type` field maps to a string in the frontend — do not create a C# enum for it unless it already exists in the project.

---

## API contract

**Base URL (dev):** `http://localhost:8080/api`
**Base URL (prod):** `http://[SERVER_IP]:8080/api`
**Content-Type:** `application/json`
**Charset:** UTF-8
**Date format:** `YYYY-MM-DD`
**Timestamp format:** `YYYY-MM-DDTHH:mm:ss`
**Currency:** `decimal` with 2 decimal places — display as `$209.100` (Colombian peso format, period as thousands separator)

### Standard response envelope

Every successful response:
```json
{ "data": { }, "timestamp": "2026-01-28T10:15:00" }
```

Every error response:
```json
{ "code": "ERROR_CODE", "message": "Descripción del error", "timestamp": "2026-01-28T10:15:00" }
```

### Error codes

| Code | HTTP | Frontend behavior |
|---|---|---|
| `VEHICLE_NOT_FOUND` | 404 | Not an error — means new plate, keep model field editable |
| `MECHANIC_NOT_FOUND` | 404 | Show error message |
| `INVOICE_NOT_FOUND` | 404 | Show error message |
| `INVOICE_ALREADY_CANCELLED` | 409 | Show error message |
| `LIQUIDATION_ALREADY_EXISTS` | 409 | Show error message |
| `VALIDATION_ERROR` | 400 | Show validation message from response body |
| `INTERNAL_ERROR` | 500 | Show generic error message |

---

### Mechanics · `/mechanics`

#### GET /mechanics
```
Query: active (bool, default true)
Returns: List<Mechanic>

Mechanic {
    id:        long
    name:      string
    is_active: bool
}
```

#### GET /mechanics/{id}
```
Returns: Mechanic
404: MECHANIC_NOT_FOUND
```

#### POST /mechanics
```
Sends:   { "name": "Carlos" }
Returns: Mechanic (id assigned by backend, is_active: true)
400: VALIDATION_ERROR
```

#### PATCH /mechanics/{id}/status
```
Sends:   { "is_active": false }
Returns: Mechanic
404: MECHANIC_NOT_FOUND
```

---

### Vehicles · `/vehicles`

#### GET /vehicles/plate/{plate}
```
Path param: plate (string) — send uppercase, no spaces
Returns: Vehicle
404: VEHICLE_NOT_FOUND

Vehicle {
    id:    long
    plate: string
    model: string
}
```

**Special behavior:** `VEHICLE_NOT_FOUND` (404) is NOT an error condition. It means the plate is new. The frontend should catch this specific code and keep the model field editable for manual input. Do not show an error message to the user.

---

### Invoices · `/invoices`

#### GET /invoices
```
Query:
  date        string YYYY-MM-DD (default: today)
  type        string SERVICE | DELIVERY (default: both)
  mechanic_id long (optional)
  cancelled   bool (default: false)

Returns: List<Invoice>

Invoice {
    id:           long
    invoice_type: string  ("SERVICE" or "DELIVERY")
    mechanic:     MechanicRef?   (null for DELIVERY)
    vehicle:      VehicleRef?    (null for DELIVERY)
    buyer_name:   string?        (null for SERVICE)
    created_at:   string         (YYYY-MM-DDTHH:mm:ss)
    labor_amount: decimal
    total_amount: decimal
    is_cancelled: bool
    items:        List<InvoiceItem>
}

MechanicRef  { id: long, name: string }
VehicleRef   { id: long, plate: string, model: string }
InvoiceItem  { id: long, description: string, quantity: decimal, unit_price: decimal, subtotal: decimal }
```

#### GET /invoices/{id}
```
Returns: Invoice
404: INVOICE_NOT_FOUND
```

#### POST /invoices/service
```
Sends:
{
  "mechanic_id":  long,
  "plate":        string,   ← always uppercase
  "model":        string,
  "labor_amount": decimal,
  "items": [
    { "description": string, "quantity": decimal, "unit_price": decimal }
  ]
}

Returns: Invoice (full object)
400: VALIDATION_ERROR
404: MECHANIC_NOT_FOUND

Note: backend calculates subtotal per item and total_amount.
Note: if plate already exists, backend updates vehicle model and reuses record.
Note: if plate is new, backend creates vehicle in same transaction.
Note: invoice lives in frontend memory until this call — this is the single confirmation call.
```

#### POST /invoices/delivery
```
Sends:
{
  "buyer_name": string,
  "items": [
    { "description": string, "quantity": decimal, "unit_price": decimal }
  ]
}

Returns: Invoice (mechanic: null, vehicle: null, labor_amount: 0.00)
400: VALIDATION_ERROR
```

#### PATCH /invoices/{id}/cancel
```
Sends:   {} (empty body)
Returns: { "id": long, "is_cancelled": true }
404: INVOICE_NOT_FOUND
409: INVOICE_ALREADY_CANCELLED
```

---

### Autocomplete · `/invoice-items`

#### GET /invoice-items/suggestions
```
Query:
  model string (required) — vehicle model
  q     string (required, min 2 chars) — search prefix

Returns: List<ItemSuggestion>

ItemSuggestion {
    description: string
    unit_price:  decimal
}

Special behavior:
  - ALWAYS returns 200, even if results are empty — never throws an error
  - Empty array [] means no suggestions — handle silently, do not show error
  - Frontend must debounce 300ms before calling
  - Frontend must not call with fewer than 2 characters
  - Only call for SERVICE invoices — do not call for DELIVERY
```

---

### Liquidations · `/liquidations`

#### GET /liquidations
```
Query:
  mechanic_id long (optional)
  date        string YYYY-MM-DD (optional)

Returns: List<Liquidation>

Liquidation {
    id:             long
    mechanic:       MechanicRef
    date:           string   (YYYY-MM-DD)
    invoice_count:  int
    total_revenue:  decimal (sum of labor_amount)
    mechanic_share: decimal
    shop_share:     decimal
}
```

#### POST /liquidations
```
Sends (single mechanic): { "date": "2026-01-28", "mechanic_id": 1 }
Sends (all mechanics):   { "date": "2026-01-28", "mechanic_id": null }

Returns: List<Liquidation> (array of created records)
404: MECHANIC_NOT_FOUND
409: LIQUIDATION_ALREADY_EXISTS
```

---

## Business context — read this to understand why things work this way

**Plate lookup flow:**
The user types a plate in the SERVICE invoice window. The frontend calls `GET /vehicles/plate/{plate}` after the field loses focus or after a short debounce. If 200, the model field auto-fills and becomes read-only with a visible edit button. If 404 (`VEHICLE_NOT_FOUND`), the model field stays editable for manual input. This is not an error.

**Invoice confirmation flow:**
The entire invoice lives in memory in the frontend while the user fills it out. No API calls are made during editing. The single API call happens only when the user clicks "Confirmar factura". This means `POST /invoices/service` or `POST /invoices/delivery` is called exactly once per invoice, representing the user's confirmation.

**Two invoice types:**
SERVICE invoices are for workshop jobs — they have a mechanic, a vehicle, items and labor. DELIVERY invoices are for spare parts dispatched to other shops or individual buyers — they have a buyer name and items only, no mechanic, no vehicle, no labor charge. The invoice window in the frontend conditionally shows different fields based on which type is open.

**Autocomplete scope:**
The autocomplete endpoint only searches SERVICE invoices. DELIVERY invoices use full item names that include the bike model prefix (e.g. "Suzuki 150 Palanca de Freno") — these should never appear as suggestions in a SERVICE invoice context.

**Liquidation exclusions:**
DELIVERY invoices are completely excluded from liquidation calculations. Only SERVICE invoices generate mechanic commissions. The backend enforces this — the frontend does not need to filter.

**Currency:**
All monetary values travel as `decimal` with 2 decimal places (e.g. `209100.00`). The frontend formats them as `$209.100` for display (Colombian peso — period as thousands separator, no decimal places shown). Never send formatted strings to the backend.

**String normalization:**
The backend normalizes all inputs before persisting. However, the frontend should also normalize before sending:
- Plate: `.ToUpperInvariant().Trim()` — no hyphens, no spaces
- All other text fields: `.Trim()`

---

## What NOT to do

- Do not make HTTP calls from ViewModels or Views — only from Services
- Do not hardcode URLs — always use `AppConfig.BaseUrl`
- Do not return the `ApiResponse<T>` envelope to ViewModels — always unwrap `data`
- Do not treat `VEHICLE_NOT_FOUND` as an error — handle it as a business condition
- Do not treat an empty suggestions array as an error — handle it silently
- Do not call the suggestions endpoint with fewer than 2 characters
- Do not call the suggestions endpoint for DELIVERY invoices
- Do not send formatted currency strings to the backend — always send raw decimal
- Do not modify backend controllers to match the frontend — fix the frontend model instead
- Do not add authentication, token handling, or session management
- Do not add HTTPS or certificate bypass logic
- Do not create duplicate model classes — check Models/ first
- Do not refactor ViewModels or Views unless directly required to fix a wiring issue
