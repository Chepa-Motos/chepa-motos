# ChepaMotos — Backend Copilot Instructions

You are a senior Java/Spring Boot developer working on **ChepaMotos**, the REST API that powers a billing application for a motorcycle repair shop. This document contains everything you need to understand the project and generate correct, consistent code.

Read this entire file before generating any code. Do not invent endpoints, field names, or business rules — everything is defined here. When in doubt about project structure, dependencies, or configuration, read the actual project files — they are the source of truth.

---

## Project context

**Business:** Chepa Motos is a motorcycle repair shop and spare parts store in Medellín, Colombia. The system replaces a manual paper-based billing process that had 20-30% error rates.

**Primary consumer of the API:** A .NET MAUI desktop application running on Windows inside a private LAN. There is no browser client, no public internet exposure, no external integrations.

**Stack:**
- Language: Java 25
- Framework: Spring Boot 4.0.3
- Database: PostgreSQL 18 (Docker container)
- ORM: Spring Data JPA / Hibernate
- Build: Gradle
- Analytics: Metabase OSS — connects directly to PostgreSQL, the API does not serve Metabase

---

## Architecture rules — follow these without exception

**1. Never put business logic in controllers.**
Controllers only receive requests, call the service layer, and return responses. All business logic lives in services.

**2. Never expose JPA entities directly.**
Controllers always return DTOs, never entities. Map entities to response DTOs in the service layer.

**3. All endpoints return the standard response envelope.**
Every successful response wraps its payload in `ApiResponse<T>`. Every error response uses `ApiErrorResponse`. No exceptions to this rule.

**4. All string inputs are normalized in the service layer before persisting.**
- `plate`: `.trim().toUpperCase()`
- `model`, `description`, `buyer_name`: `.trim()`
Do not rely on the frontend to send clean data.

**5. The backend calculates all monetary values.**
`subtotal` for each item = `quantity × unit_price`. `total_amount` for invoice = sum of subtotals + `labor_amount`. Never trust these values from the request body.

**6. Invoices are never deleted — only cancelled.**
`is_cancelled = true` is the only way to remove an invoice from active results. No `DELETE` endpoints exist for invoices.

**7. Use `@Transactional` on service methods that write to the database.**
Creating an invoice + its items must be a single atomic transaction. Vehicle creation or update, if needed, is part of the same transaction.

**8. The API uses HTTP, not HTTPS.**
It runs on a private LAN. Do not add SSL configuration or redirect logic.

**9. Do not add authentication or Spring Security.**
The MVP has no auth layer. Do not add `@PreAuthorize`, filters, or security configuration.

**10. Keep abstractions minimal.**
Do not create generic base classes, abstract repositories, or unnecessary design pattern infrastructure unless explicitly asked.

---

## Database schema

### Column naming convention
All primary keys use the full name pattern: `mechanic_id`, `vehicle_id`, `invoice_id`, `invoice_item_id`, `daily_liquidation_id`. Map these in JPA using `@Column(name = "mechanic_id")` etc.

### Entities

**mechanic**
| Column | Type | Constraints |
|---|---|---|
| mechanic_id | BIGINT GENERATED ALWAYS AS IDENTITY | PRIMARY KEY |
| name | VARCHAR(100) | NOT NULL |
| is_active | BOOLEAN | NOT NULL, DEFAULT true |

**vehicle**
| Column | Type | Constraints |
|---|---|---|
| vehicle_id | BIGINT GENERATED ALWAYS AS IDENTITY | PRIMARY KEY |
| plate | VARCHAR(20) | NOT NULL, UNIQUE |
| model | VARCHAR(100) | NOT NULL |

**invoice**
| Column | Type | Constraints |
|---|---|---|
| invoice_id | BIGINT GENERATED ALWAYS AS IDENTITY | PRIMARY KEY |
| invoice_type | ENUM('SERVICE','DELIVERY') | NOT NULL |
| mechanic_id | BIGINT FK | NULL — only SERVICE invoices have this |
| vehicle_id | BIGINT FK | NULL — only SERVICE invoices have this |
| buyer_name | VARCHAR(150) | NULL — only DELIVERY invoices have this |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |
| labor_amount | NUMERIC(10,2) | NOT NULL, DEFAULT 0 |
| total_amount | NUMERIC(10,2) | NOT NULL, DEFAULT 0 |
| is_cancelled | BOOLEAN | NOT NULL, DEFAULT false |

**invoice_item**
| Column | Type | Constraints |
|---|---|---|
| invoice_item_id | BIGINT GENERATED ALWAYS AS IDENTITY | PRIMARY KEY |
| invoice_id | BIGINT FK | NOT NULL |
| description | VARCHAR(255) | NOT NULL — stored trimmed |
| quantity | NUMERIC(10,2) | NOT NULL |
| unit_price | NUMERIC(10,2) | NOT NULL |
| subtotal | NUMERIC(10,2) | NOT NULL — calculated by backend, never trusted from request |

**daily_liquidation**
| Column | Type | Constraints |
|---|---|---|
| daily_liquidation_id | BIGINT GENERATED ALWAYS AS IDENTITY | PRIMARY KEY |
| mechanic_id | BIGINT FK | NOT NULL |
| date | DATE | NOT NULL |
| total_revenue | NUMERIC(10,2) | NOT NULL |
| mechanic_share | NUMERIC(10,2) | NOT NULL |
| shop_share | NUMERIC(10,2) | NOT NULL |
| invoice_count | INTEGER | NOT NULL |
| — | UNIQUE(mechanic_id, date) | prevents double liquidation per mechanic per day |

### PostgreSQL ENUM type
The `invoice_type` column uses a native PostgreSQL ENUM type named `invoice_type`. With Hibernate 6 (included in Spring Boot 4), map it as:

```java
@Enumerated(EnumType.STRING)
@Column(name = "invoice_type", columnDefinition = "invoice_type")
private InvoiceType invoiceType;
```

---

## Business rules

**Invoice types:**
- `SERVICE`: must have `mechanic_id` + `vehicle_id`. `buyer_name` must be null. `labor_amount` >= 0.
- `DELIVERY`: must have `buyer_name`. `mechanic_id` and `vehicle_id` must be null. `labor_amount` is always 0 — ignore any value sent in the request and always persist 0.
- The database enforces this via a CHECK constraint. The service layer must also validate before hitting the DB.

**Vehicle logic on SERVICE invoice:**
- If the plate does NOT exist → create a new vehicle record with the plate and model from the request, in the same transaction as the invoice.
- If the plate EXISTS → update the vehicle's model with the value from the request, then reuse the record. This allows intentional model corrections. The frontend blocks editing after plate auto-fill and requires an explicit edit action before allowing model changes — so any model value sent with an existing plate is intentional.

**Invoice cancellation:**
Sets `is_cancelled = true`. Irreversible. If already cancelled, return `INVOICE_ALREADY_CANCELLED` (409). Never delete the record.

**Daily liquidation logic:**
1. Find all active mechanics who have active SERVICE invoices on the given date (or just the specified mechanic if `mechanic_id` is provided).
2. For each mechanic:
   - Check no liquidation exists for `mechanic_id + date` — if it does, return `LIQUIDATION_ALREADY_EXISTS` (409).
   - Sum `labor_amount` of all `is_cancelled = false` SERVICE invoices for that mechanic on that date.
   - This sum is `total_revenue`  . 
   - `mechanic_share` = `total_revenue × 0.70`
   - `shop_share` = `total_revenue × 0.30`
   - `invoice_count` = count of those invoices.
   - Persist the `daily_liquidation` record.
3. Return an array of all created liquidation objects.
- DELIVERY invoices are completely excluded — they generate no commission.

**Autocomplete suggestions:**
- Query `invoice_item` → join `invoice` → join `vehicle`.
- Filter: `invoice_type = 'SERVICE'`, `is_cancelled = false`, `LOWER(vehicle.model) ILIKE LOWER(:model || '%')`, `LOWER(description) ILIKE LOWER(:q || '%')`.
- Group by description, return the most recent `unit_price` for each unique description.
- Order by frequency (count) descending.
- Return max 10 results.
- Return empty array `[]` if no results — never throw an exception for empty results.
- The trigram index on `LOWER(description)` in PostgreSQL makes this query fast at scale.

---

## Standard response envelope

Every endpoint uses these wrappers without exception.

```java
public class ApiResponse<T> {
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> of(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.data = data;
        response.timestamp = LocalDateTime.now().toString();
        return response;
    }
}

public class ApiErrorResponse {
    private String code;
    private String message;
    private String timestamp;
}
```

Implement a `@RestControllerAdvice` global exception handler that catches custom exceptions and maps them to `ApiErrorResponse`. Never let stack traces reach the client.

---

## Error codes

| Code | HTTP | When to use |
|---|---|---|
| `VEHICLE_NOT_FOUND` | 404 | Plate lookup returns no result — expected business condition, do not log as error |
| `MECHANIC_NOT_FOUND` | 404 | `mechanic_id` does not exist |
| `INVOICE_NOT_FOUND` | 404 | Invoice ID does not exist |
| `INVOICE_ALREADY_CANCELLED` | 409 | Cancelling an already-cancelled invoice |
| `LIQUIDATION_ALREADY_EXISTS` | 409 | Liquidation already exists for mechanic + date |
| `VALIDATION_ERROR` | 400 | Bean validation failures (`@Valid`) |
| `INTERNAL_ERROR` | 500 | Unexpected exceptions |

---

## API contract

**Base URL (dev):** `http://localhost:8080/api`
**Base URL (prod):** `http://[SERVER_IP]:8080/api`
**Content-Type:** `application/json`
**Charset:** UTF-8
**Date format:** `YYYY-MM-DD`
**Timestamp format:** `YYYY-MM-DDTHH:mm:ss`
**Currency:** `BigDecimal` with 2 decimal places — e.g. `209100.00`. Frontend is responsible for display formatting (`$209.100`).

---

### Mechanics · `/mechanics`

#### GET /mechanics
```
Query: active (boolean, default true)
Response 200:
{
  "data": [{ "id": 1, "name": "Jose", "is_active": true }],
  "timestamp": "2026-01-28T10:15:00"
}
```

#### GET /mechanics/{id}
```
Response 200: { "data": { "id": 1, "name": "Jose", "is_active": true } }
Response 404: MECHANIC_NOT_FOUND
```

#### POST /mechanics
```
Request:    { "name": "Carlos" }
Validation: name required, not blank, max 100 chars
Response 201: { "data": { "id": 6, "name": "Carlos", "is_active": true } }
Response 400: VALIDATION_ERROR
```

#### PATCH /mechanics/{id}/status
```
Request:  { "is_active": false }
Response 200: { "data": { "id": 1, "name": "Jose", "is_active": false } }
Response 404: MECHANIC_NOT_FOUND
```

---

### Vehicles · `/vehicles`

#### GET /vehicles/plate/{plate}
```
Path param: plate (String) — normalize to uppercase before querying
Response 200: { "data": { "id": 3, "plate": "BXR74F", "model": "Boxer 150 2021" } }
Response 404: VEHICLE_NOT_FOUND
```
Note: 404 here means the plate is new. The frontend handles this gracefully by keeping the model field editable. Do not log it as an error.

---

### Invoices · `/invoices`

#### GET /invoices
```
Query:
  date        (YYYY-MM-DD, default: today)
  type        (SERVICE | DELIVERY, default: both)
  mechanic_id (Long, optional)
  cancelled   (boolean, default: false)

Response 200:
{
  "data": [{
    "id": 14,
    "invoice_type": "SERVICE",
    "mechanic": { "id": 1, "name": "Jose" },
    "vehicle": { "id": 3, "plate": "BXR74F", "model": "Boxer 150 2021" },
    "buyer_name": null,
    "created_at": "2026-01-28T09:45:00",
    "labor_amount": 65000.00,
    "total_amount": 209100.00,
    "is_cancelled": false,
    "items": [
      { "id": 51, "description": "Tornillo Leva", "quantity": 1, "unit_price": 3900.00, "subtotal": 3900.00 }
    ]
  }]
}
```

#### GET /invoices/{id}
```
Response 200: same structure as single element above
Response 404: INVOICE_NOT_FOUND
```

#### POST /invoices/service
```
Request:
{
  "mechanic_id": 1,
  "plate": "BXR74F",
  "model": "Boxer 150 2021",
  "labor_amount": 65000.00,
  "items": [
    { "description": "Tornillo Leva", "quantity": 1, "unit_price": 3900.00 }
  ]
}

Validations:
  mechanic_id:         required — mechanic must exist and be active
  plate:               required, not blank, max 20 chars — normalize to uppercase
  model:               required, not blank, max 100 chars — trim
  labor_amount:        required, >= 0
  items:               required, min 1 item
  items[].description: required, not blank, max 255 chars — trim
  items[].quantity:    required, > 0
  items[].unit_price:  required, >= 0

Backend calculates:
  items[].subtotal = quantity × unit_price
  total_amount     = sum(subtotals) + labor_amount

Vehicle logic:
  plate EXISTS     → update vehicle.model with value from request, reuse the record
  plate NOT FOUND  → create new vehicle (plate + model) in same transaction

Response 201: full invoice object
Response 400: VALIDATION_ERROR
Response 404: MECHANIC_NOT_FOUND
```

#### POST /invoices/delivery
```
Request:
{
  "buyer_name": "Talleres La 80",
  "items": [
    { "description": "Suzuki 150 Palanca de Freno", "quantity": 2, "unit_price": 18500.00 }
  ]
}

Validations:
  buyer_name: required, not blank, max 150 chars — trim
  items:      required, min 1 item
  Same item validations as SERVICE

labor_amount is always 0 — always persist as 0, ignore any value in request
mechanic and vehicle are always null

Response 201: full invoice object (mechanic: null, vehicle: null, labor_amount: 0.00)
Response 400: VALIDATION_ERROR
```

#### PATCH /invoices/{id}/cancel
```
Request: {} (empty body)
Response 200: { "data": { "id": 14, "is_cancelled": true } }
Response 404: INVOICE_NOT_FOUND
Response 409: INVOICE_ALREADY_CANCELLED
```

---

### Autocomplete · `/invoice-items`

#### GET /invoice-items/suggestions
```
Query:
  model (String, required) — vehicle model to scope the search
  q     (String, required, min 2 chars) — search prefix

Response 200:
{
  "data": [
    { "description": "Freno Delantero", "unit_price": 45000.00 },
    { "description": "Freno Trasero",   "unit_price": 38000.00 }
  ]
}

Rules:
  - Only SERVICE invoices (invoice_type = 'SERVICE')
  - Only active invoices (is_cancelled = false)
  - Uses LOWER(description) ILIKE LOWER(:q || '%') — leverages trigram index
  - Returns max 10 results ordered by frequency descending
  - Returns [] if no results — never an error, never throw an exception
```

---

### Liquidations · `/liquidations`

#### GET /liquidations
```
Query:
  mechanic_id (Long, optional)
  date        (YYYY-MM-DD, optional)

Response 200:
{
  "data": [{
    "id": 8,
    "mechanic": { "id": 1, "name": "Jose" },
    "date": "2026-01-28",
    "invoice_count": 4,
    "total_revenue": 304100.00,
    "mechanic_share": 212870.00,
    "shop_share": 91230.00
  }]
}
```

#### POST /liquidations
```
Request (single mechanic): { "date": "2026-01-28", "mechanic_id": 1 }
Request (all mechanics):   { "date": "2026-01-28", "mechanic_id": null }

Response 201: array of created liquidation objects
Response 404: MECHANIC_NOT_FOUND
Response 409: LIQUIDATION_ALREADY_EXISTS
```

---

## Endpoint summary

| Method | Path | Description |
|---|---|---|
| GET | `/mechanics` | List mechanics |
| GET | `/mechanics/{id}` | Get mechanic by ID |
| POST | `/mechanics` | Create mechanic |
| PATCH | `/mechanics/{id}/status` | Activate / deactivate mechanic |
| GET | `/vehicles/plate/{plate}` | Lookup vehicle by plate |
| GET | `/invoices` | List invoices with filters |
| GET | `/invoices/{id}` | Get invoice by ID |
| POST | `/invoices/service` | Create SERVICE invoice |
| POST | `/invoices/delivery` | Create DELIVERY invoice |
| PATCH | `/invoices/{id}/cancel` | Cancel invoice |
| GET | `/invoice-items/suggestions` | Autocomplete suggestions |
| GET | `/liquidations` | List liquidations |
| POST | `/liquidations` | Execute daily liquidation |

---

## What NOT to do

- Do not add Spring Security, JWT, or any authentication layer
- Do not use `ddl-auto=create` or `ddl-auto=update` — schema is managed exclusively by `schema.sql`
- Do not expose JPA entities in controller responses — always use DTOs
- Do not add Flyway or Liquibase unless explicitly asked
- Do not add MapStruct or ModelMapper — map manually with simple methods
- Do not add Swagger/OpenAPI unless explicitly asked
- Do not add caching (Redis, Caffeine, etc.)
- Do not add `@DeleteMapping` endpoints for invoices — cancellation only, never deletion
- Do not trust `subtotal` or `total_amount` from the request body — always recalculate
- Do not log `VEHICLE_NOT_FOUND` as an error — it is an expected business condition
- Do not add HTTPS, SSL certificates, or port 443 configuration
- Do not suggest architectural refactors outside the current task scope
- Do not use Maven — this project uses Gradle
