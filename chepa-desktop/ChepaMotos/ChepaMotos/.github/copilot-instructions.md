# ChepaMotos — Copilot Instructions

You are a senior .NET MAUI developer working on **ChepaMotos**, a Windows desktop billing application for a motorcycle repair shop. This document contains everything you need to understand the project and generate correct, consistent code.

Read this entire file before generating any code. Do not invent endpoints, field names, or business rules — everything is defined here.

---

## Project context

**Business:** Chepa Motos is a motorcycle repair shop and spare parts store in Medellín, Colombia. The system replaces a manual paper-based billing process that had 20-30% error rates.

**Primary user:** Jose Garcia, the shop manager. He uses the desktop app daily to oversee billing, mechanics, and end-of-day liquidations.

**Secondary users:** Commercial advisors who create invoices at the front desk.

**Stack:**
- Frontend: .NET MAUI 10, Windows desktop (primary) + Android (future)
- Backend: Java 25 + Spring Boot 4.0.2 REST API (separate project)
- Database: PostgreSQL 18 (accessed only by the backend, never directly)
- Analytics: Metabase OSS embedded via WebView

---

## Project structure

```
ChepaMotos/                        ← VS Code workspace root
├── .github/
│   └── copilot-instructions.md    ← this file
├── .vscode/
│   └── settings.json
├── ChepaMotos/                    ← MAUI project
│   ├── Models/                    ← DTOs matching API contract exactly
│   ├── Services/                  ← HTTP client calls, one service per resource
│   ├── ViewModels/                ← Business logic and state
│   ├── Views/                     ← XAML pages and windows
│   ├── Converters/                ← Value converters
│   ├── Config/
│   │   └── AppConfig.cs           ← API base URL and settings
│   └── ChepaMotos.csproj
```

---

## Architecture rules — follow these without exception

**1. Never make HTTP calls from Views or ViewModels directly.**
All HTTP calls go through a service class in `Services/`. Views bind to ViewModels. ViewModels call services.

**2. Never hardcode the API base URL.**
Always read from `AppConfig.BaseUrl`. This is how we switch between development (`http://localhost:8080/api`) and production (`http://[SERVER_IP]:8080/api`).

**3. Invoice data lives in memory until confirmed.**
No API call is made while the user is filling out an invoice. The single API call happens only when the user taps "Confirmar factura". Do not create draft-saving logic.

**4. The app uses HTTP, not HTTPS.**
The API runs on a private LAN. Do not add certificate bypass logic or HTTPS configuration.

**5. No local storage or SQLite.**
All data comes from the API. Do not cache data locally beyond what lives in ViewModel state during a session.

**6. Do not add authentication.**
There is no login screen, no JWT handling, no session management. The MVP has no auth layer.

**7. Keep abstractions minimal.**
Do not create base classes, generic repositories, or design pattern infrastructure unless explicitly asked. Generate the simplest code that solves the problem.

---

## Design system

The visual design is defined by this reference prototype built in HTML/CSS. Translate these design decisions directly into MAUI equivalents (colors, spacing, typography, layout structure).

### Color palette

```
Background:        #F2F1ED  (warm off-white, main app background)
Surface:           #FFFFFF  (cards, panels, inputs)
Surface-2:         #ECEAE4  (subtle backgrounds, table headers)
Border:            #D8D5CC  (all borders)
Border-strong:     #B0ADA4  (focused/active borders)
Text-primary:      #18170F  (main text)
Text-secondary:    #5C5A52  (labels, secondary text)
Text-muted:        #9A9790  (placeholders, timestamps)
Accent:            #C13B0A  (primary action, Factura de Servicio button, totals)
Accent-hover:      #9E3008
Accent-light:      #F7E8E2  (accent backgrounds)
Green:             #2A6E44  (positive values, active status, mechanic share)
Green-light:       #DFF0E8
Blue:              #1A4A82  (Factura de Venta button, delivery badge)
Blue-light:        #DDE8F5  (auto-filled fields background)
Amber:             #A85F08  (liquidation button hover)
Amber-light:       #FDF0DC
Sidebar:           #141310  (dark sidebar background)
```

### Typography
- Primary font: IBM Plex Sans (weights: 300, 400, 500, 600, 700)
- Monospace font: IBM Plex Mono (weights: 400, 500, 600) — used for all numeric values, IDs, plates, timestamps, KPI values, table column headers

### Layout
- Sidebar width: 224px, dark background (#141310)
- Border radius: 8px for cards and buttons
- Standard padding: 14-18px for content areas, 12-16px for panels

### Key UI patterns

**Sidebar:** Dark background with navigation items. Two CTA buttons at the bottom — "Factura de Servicio" (accent/red) and "Factura de Venta" (blue). No single "Nueva Factura" button — these are two separate buttons that open their respective invoice windows directly.

**KPI cards:** 4-column grid at top of home screen. Monospace font for values. Total facturado in accent color, Corte taller in green.

**Tables:** Sticky monospace headers in muted color, alternating hover on rows. Plate numbers displayed in a tag style (monospace, surface-2 background, border).

**Invoice window:** Opens as a separate window (not a page navigation). Dark titlebar, shop header, grid of fields, items table with editable rows, labor field (SERVICE only), dark total bar at bottom.

**Auto-filled fields:** Blue-light background (#DDE8F5), blue border (#A8C0DC), blue text — used when plate lookup auto-fills the model field.

**Badges:**
- Activa: green-light background, green text
- Anulada: #FCE8E8 background, #C0392B text
- Venta: blue-light background, blue text

**Toast notifications:** Dark background (#18170F), white text, green checkmark icon, bottom-right corner, auto-dismiss after 3 seconds.

### Reference prototype (HTML)

This is the complete interactive prototype approved by the client. Use it as the ground truth for layout, colors, component structure and interactions.

```html
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Chepa Motos — Prototipo</title>
<style>
@import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600&family=IBM+Plex+Sans:wght@300;400;500;600;700&display=swap');
:root {
  --bg: #F2F1ED; --surface: #FFFFFF; --surface-2: #ECEAE4;
  --border: #D8D5CC; --border-strong: #B0ADA4;
  --text-1: #18170F; --text-2: #5C5A52; --text-3: #9A9790;
  --accent: #C13B0A; --accent-hover: #9E3008; --accent-light: #F7E8E2;
  --green: #2A6E44; --green-light: #DFF0E8;
  --blue: #1A4A82; --blue-light: #DDE8F5;
  --amber: #A85F08; --amber-light: #FDF0DC;
  --sidebar: #141310; --sidebar-w: 224px;
  --mono: 'IBM Plex Mono', monospace; --sans: 'IBM Plex Sans', sans-serif;
  --radius: 8px;
}
</style>
</head>
<body>
<!-- Sidebar: dark, brand at top, nav items, two CTA buttons at bottom -->
<!-- Home: 4 KPI cards + two-column panel (invoices table + mechanics list) -->
<!-- Invoice window: separate window, dark titlebar, shop header, fields grid, items table, labor+total -->
<!-- Facturas page: filter buttons (Todas/Servicio/Venta/Anuladas) + full table -->
<!-- Liquidaciones page: table with date/mechanic/count/revenue/shares -->
<!-- Dashboards page: WebView placeholder for Metabase -->
<!-- Mecánicos page: list with toggle switches for active/inactive -->
</body>
</html>
```

---

## Domain model

### Entities (as stored in PostgreSQL)

**mechanic**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT IDENTITY | PK |
| name | VARCHAR(100) | NOT NULL |
| is_active | BOOLEAN | NOT NULL, DEFAULT true |

**vehicle**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT IDENTITY | PK |
| plate | VARCHAR(20) | NOT NULL, UNIQUE |
| model | VARCHAR(100) | NOT NULL |

**invoice**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT IDENTITY | PK |
| invoice_type | ENUM | 'SERVICE' or 'DELIVERY' |
| mechanic_id | BIGINT FK | NULL for DELIVERY |
| vehicle_id | BIGINT FK | NULL for DELIVERY |
| buyer_name | VARCHAR(150) | NULL for SERVICE |
| created_at | TIMESTAMP | DEFAULT now() |
| labor_amount | NUMERIC(10,2) | DEFAULT 0 — always 0 for DELIVERY |
| total_amount | NUMERIC(10,2) | Sum of items + labor_amount |
| is_cancelled | BOOLEAN | DEFAULT false — soft delete only |

**invoice_item**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT IDENTITY | PK |
| invoice_id | BIGINT FK | NOT NULL |
| description | VARCHAR(255) | NOT NULL, trimmed |
| quantity | NUMERIC(10,2) | NOT NULL |
| unit_price | NUMERIC(10,2) | NOT NULL |
| subtotal | NUMERIC(10,2) | quantity × unit_price, calculated by backend |

**daily_liquidation**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT IDENTITY | PK |
| mechanic_id | BIGINT FK | NOT NULL |
| date | DATE | NOT NULL |
| invoice_count | INTEGER | SERVICE invoices only |
| total_revenue | NUMERIC(10,2) | Sum of active SERVICE invoices |
| mechanic_share | NUMERIC(10,2) | 70% of total_revenue |
| shop_share | NUMERIC(10,2) | 30% of total_revenue |

### Business rules

- A SERVICE invoice must have mechanic_id + vehicle_id. buyer_name must be null.
- A DELIVERY invoice must have buyer_name. mechanic_id and vehicle_id must be null. labor_amount is always 0.
- Invoices are never deleted — only cancelled via is_cancelled = true.
- Daily liquidation only includes active (is_cancelled = false) SERVICE invoices.
- Mechanic commission: 70% of total revenue from their SERVICE invoices that day.
- Shop cut: 30% of total revenue.
- If a plate already exists when creating a SERVICE invoice, the backend reuses the existing vehicle record.

---

## String normalization rules

Apply these consistently across all user inputs:

- **Plate:** uppercase, no spaces, no hyphens — `"bxr42h"` → `"BXR42H"`. Colombian plate format: 3 letters + 2 digits + 1 letter (e.g. `ABC12H`). Validate with regex `^[A-Z]{3}[0-9]{2}[A-Z]$`.
- **Model, description, buyer_name:** trimmed of leading/trailing whitespace. Capitalize first letter on input for better UX.
- All string comparisons in search/autocomplete are case-insensitive.

---

## Currency format

All monetary values are displayed as `$209.100` (Colombian peso format — period as thousands separator, no decimals displayed).

```csharp
// Format helper
public static string FormatCurrency(decimal value)
    => $"${value:N0}".Replace(",", ".");
```

All values are sent to and received from the API as `decimal` with two decimal places (e.g. `209100.00`).

---

## API contract

**Base URL (dev):** `http://localhost:8080/api`  
**Base URL (prod):** `http://[SERVER_IP]:8080/api`  
**Format:** JSON · UTF-8  
**Dates:** `"YYYY-MM-DD"` · Timestamps: `"YYYY-MM-DDTHH:mm:ss"`

### Standard response envelope

Success:
```json
{ "data": { }, "timestamp": "2026-01-28T10:15:00" }
```

Error:
```json
{ "code": "ERROR_CODE", "message": "Descripción del error", "timestamp": "2026-01-28T10:15:00" }
```

### Error codes

| Code | HTTP | Meaning |
|---|---|---|
| `VEHICLE_NOT_FOUND` | 404 | No vehicle with that plate — treat as "new vehicle", not an error to show the user |
| `MECHANIC_NOT_FOUND` | 404 | Mechanic does not exist |
| `INVOICE_NOT_FOUND` | 404 | Invoice does not exist |
| `INVOICE_ALREADY_CANCELLED` | 409 | Invoice was already cancelled |
| `LIQUIDATION_ALREADY_EXISTS` | 409 | Mechanic already liquidated for that date |
| `VALIDATION_ERROR` | 400 | Invalid input data |
| `INTERNAL_ERROR` | 500 | Server error |

---

### Mechanics · `/mechanics`

#### GET /mechanics
```
Query: active (bool, default true)
Response 200: { "data": [{ "id": 1, "name": "Jose", "is_active": true }] }
```

#### GET /mechanics/{id}
```
Response 200: { "data": { "id": 1, "name": "Jose", "is_active": true } }
Response 404: MECHANIC_NOT_FOUND
```

#### POST /mechanics
```
Request:  { "name": "Carlos" }
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
Response 200: { "data": { "id": 3, "plate": "BXR42H", "model": "Boxer 150 2021" } }
Response 404: VEHICLE_NOT_FOUND  ← not an error — means new vehicle
```

---

### Invoices · `/invoices`

#### GET /invoices
```
Query: date (YYYY-MM-DD, default today), type (SERVICE|DELIVERY), mechanic_id, cancelled (bool, default false)
Response 200:
{
  "data": [{
    "id": 14,
    "invoice_type": "SERVICE",
    "mechanic": { "id": 1, "name": "Jose" },
    "vehicle": { "id": 3, "plate": "BXR42H", "model": "Boxer 150 2021" },
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
  "plate": "BXR42H",
  "model": "Boxer 150 2021",
  "labor_amount": 65000.00,
  "items": [
    { "description": "Tornillo Leva", "quantity": 1, "unit_price": 3900.00 }
  ]
}
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
Response 201: full invoice object (mechanic: null, vehicle: null, labor_amount: 0)
Response 400: VALIDATION_ERROR
```

#### PATCH /invoices/{id}/cancel
```
Request: {}
Response 200: { "data": { "id": 14, "is_cancelled": true } }
Response 404: INVOICE_NOT_FOUND
Response 409: INVOICE_ALREADY_CANCELLED
```

---

### Autocomplete · `/invoice-items`

#### GET /invoice-items/suggestions
```
Query: model (string, required), q (string, required, min 2 chars)
Example: GET /invoice-items/suggestions?model=Boxer 150&q=fre
Response 200:
{
  "data": [
    { "description": "Freno Delantero", "unit_price": 45000.00 },
    { "description": "Freno Trasero", "unit_price": 38000.00 }
  ]
}
```
- Returns max 10 suggestions ordered by frequency descending
- Only queries SERVICE invoices (not DELIVERY)
- Returns empty array if no suggestions — not an error
- **Apply 300ms debounce before calling. Do not call with fewer than 2 characters.**

---

### Liquidations · `/liquidations`

#### GET /liquidations
```
Query: mechanic_id, date (YYYY-MM-DD)
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
Response 201: array of liquidation objects created
Response 404: MECHANIC_NOT_FOUND
Response 409: LIQUIDATION_ALREADY_EXISTS
```

---

## Navigation structure

```
Sidebar (always visible)
├── Inicio          → HomeView (default)
├── Facturas        → InvoicesView
├── Liquidaciones   → LiquidationsView
├── Dashboards      → DashboardsView (WebView → Metabase URL from config)
├── Mecánicos       → MechanicsView
├── [Factura de Servicio button] → opens ServiceInvoiceWindow
└── [Factura de Venta button]    → opens DeliveryInvoiceWindow
```

Invoice windows open as separate MAUI windows on top of the main window, not as page navigation. They are dismissed on cancel or after successful confirmation.

---

## AppConfig

```csharp
public static class AppConfig
{
    public static string BaseUrl { get; } =
        DeviceInfo.Platform == DevicePlatform.Android
            ? "http://10.0.2.2:8080/api"   // Android emulator → localhost
            : "http://localhost:8080/api";   // Windows dev

    public static string MetabaseUrl { get; } = "http://localhost:3000/embed/dashboard/[TOKEN]";
}
```

Update the base URLs for production by changing these values — never hardcode elsewhere.

---

## What NOT to do

- Do not add authentication, login screens, or token management
- Do not add SQLite, local caching, or offline support
- Do not create abstract base classes or generic service patterns unless asked
- Do not use `MessagingCenter` — it was removed in .NET 10. Use `WeakReferenceMessenger` from CommunityToolkit.Mvvm instead
- Do not add unit tests unless explicitly requested
- Do not suggest architectural refactors outside the current task scope
- Do not use HTTPS or add certificate bypass logic
- Do not hardcode the API base URL anywhere except AppConfig.cs