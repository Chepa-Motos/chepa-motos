# DOCUMENTO DE LEVANTAMIENTO DE REQUISITOS
## ChepaMotos - Sistema REST de Facturación y Gestión

---

## 1. CONTEXTO DEL NEGOCIO

### 1.1 Descripción General
**ChepaMotos** es un taller de reparación de motocicletas y comercio de repuestos ubicado en Medellín, Colombia. El negocio ha operado tradicionalmente utilizando un sistema manual de facturas en papel que generaba una tasa de error de **20-30%**.

### 1.2 Problemática
- **Error manual:** Las facturas en papel son propensas a errores de cálculo
- **Falta de control:** No hay seguimiento centralizado de transacciones
- **Ineficiencia:** Cada factura requiere escritura manual y verificación
- **Pérdida de datos:** Las facturas físicas son difíciles de archivar y recuperar

### 1.3 Solución Propuesta
Implementar un sistema automatizado de facturación que:
- Elimine errores de cálculo mediante cálculo automático de subtotales y totales
- Centralice toda la información de transacciones en una base de datos
- Automatice procesos de liquidación y comisiones
- Proporcione trazabilidad completa de todas las operaciones

### 1.4 Alcance
**In Scope (Incluido):**
- Sistema REST API para gestión de facturas (servicio y entrega)
- Gestión de mecánicos y sus comisiones
- Gestión de vehículos y sus historiales
- Cálculo automático de liquidaciones diarias
- Base de datos PostgreSQL

**Out of Scope (No incluido):**
- Autenticación y autorización
- Integración con sistemas externos
- Funcionalidad de pago en línea

---

## 2. OBJETIVOS DEL PROYECTO

### 2.1 Objetivo General
Desarrollar una API REST segura y confiable que reemplace el sistema manual de facturación de ChepaMotos, reduciendo errores y mejorando la eficiencia operativa.

### 2.2 Objetivos Específicos
1. **Automatización:** Automatizar el cálculo de subtotales, totales e comisiones
2. **Integridad:** Garantizar consistencia de datos mediante validaciones y transacciones atómicas
3. **Trazabilidad:** Mantener un registro irrevocable de todas las transacciones
4. **Disponibilidad:** Proporcionar disponibilidad 24/7 dentro de la red privada
5. **Rendimiento:** Procesar solicitudes en < 200ms en condiciones normales

### 2.3 Beneficios Esperados
- Reducción de errores del 95%
- Ahorro de 10+ horas de trabajo administrativo por semana
- Mejor control de comisiones de mecánicos (precisión del 100%)
- Consultas rápidas de historial de facturas

---

## 3. REQUISITOS FUNCIONALES

### 3.1 Gestión de Mecánicos
**RF-001: Listar Mecánicos**
- El sistema debe permitir listar todos los mecánicos activos
- Opcionalmente filtrar por estado activo/inactivo
- Endpoint: `GET /api/mechanics`

**RF-002: Obtener Mecánico por ID**
- El sistema debe retornar detalles de un mecánico específico
- Retornar error 404 si no existe
- Endpoint: `GET /api/mechanics/{id}`

**RF-003: Crear Mecánico**
- El sistema debe permitir registrar nuevos mecánicos
- Validar nombre (requerido, no vacío, máx 100 caracteres)
- Los mecánicos inician activos por defecto
- Endpoint: `POST /api/mechanics`

**RF-004: Cambiar Estatus de Mecánico**
- El sistema debe permitir activar o desactivar mecánicos
- Un mecánico inactivo no puede ser asignado a nuevas facturas
- Endpoint: `PATCH /api/mechanics/{id}/status`

### 3.2 Gestión de Vehículos
**RF-005: Buscar Vehículo por Placa**
- El sistema debe permitir consultar un vehículo por su placa
- Normalizar placa a mayúsculas antes de consulta
- Si no existe, retornar 404 (es una condición esperada, no error)
- Endpoint: `GET /api/vehicles/plate/{plate}`

**RF-006: Crear o Actualizar Vehículo**
- Si la placa NO existe → crear nuevo vehículo
- Si la placa EXISTE → actualizar el modelo (permite correcciones)
- Esta acción ocurre automáticamente al crear factura de servicio
- Transacción atómica con la factura

### 3.3 Gestión de Facturas (Servicio)
**RF-007: Crear Factura de Servicio**
- Requerimientos:
  - Mecánico ID (debe existir y estar activo)
  - Placa vehículo (requerida, máx 20 caracteres)
  - Modelo vehículo (requerido, máx 100 caracteres)
  - Monto de labor (requerido, >= 0)
  - Ítems (mínimo 1, cada uno con descripción, cantidad, precio unitario)
- Cálculos automáticos:
  - Subtotal ítem = cantidad × precio unitario
  - Total factura = suma(subtotales) + monto labor
- Vehículo: crear si no existe, actualizar modelo si existe
- Endpoint: `POST /api/invoices/service`
- Status: 201 (Created)

### 3.4 Gestión de Facturas (Entrega)
**RF-008: Crear Factura de Entrega**
- Requerimientos:
  - Nombre comprador (requerido, máx 150 caracteres)
  - Ítems (mínimo 1, mismas validaciones que servicio)
- Especificaciones:
  - Monto labor siempre = 0 (se ignora si viene en request)
  - Sin mecánico ni vehículo asociado
  - Total = suma(subtotales) únicamente
- Endpoint: `POST /api/invoices/delivery`
- Status: 201 (Created)

### 3.5 Consulta de Facturas
**RF-009: Listar Facturas**
- Filtros opcionales:
  - `date`: Fecha específica (defecto: hoy)
  - `type`: SERVICE o DELIVERY (defecto: ambas)
  - `mechanic_id`: ID de mecánico
  - `cancelled`: true/false (defecto: false, solo activas)
- Retornar lista completa con ítems y detalles
- Endpoint: `GET /api/invoices`

**RF-010: Obtener Factura por ID**
- Retornar factura con todos sus ítems
- Retornar error 404 si no existe
- Endpoint: `GET /api/invoices/{id}`

### 3.6 Cancelación de Facturas
**RF-011: Cancelar Factura**
- Marcar factura como cancelada (`is_cancelled = true`)
- Acción irreversible
- Error si ya está cancelada (409 Conflict)
- No eliminar registros (auditoría)
- Endpoint: `PATCH /api/invoices/{id}/cancel`

### 3.7 Autosugencias
**RF-012: Autosugencias de Ítems**
- El sistema debe proporcionar sugerencias de descripción y precio
- Filtros:
  - Modelo de vehículo (requerido)
  - Prefijo de búsqueda (mínimo 2 caracteres)
- Criterios:
  - Solo facturas de SERVICIO (`invoice_type = SERVICE`)
  - Solo facturas activas (`is_cancelled = false`)
  - Búsqueda case-insensitive con ILIKE
  - Máx 10 resultados
  - Ordenados por frecuencia descendente
  - Retornar descripción y precio unitario más reciente
- Retornar array vacío si no hay resultados (nunca error)
- Endpoint: `GET /api/invoice-items/suggestions`

### 3.8 Gestión de Liquidaciones
**RF-013: Crear Liquidación Diaria**
- Procesar liquidaciones para date específica
- Opciones:
  - `mechanic_id` especificado: liquidar solo ese mecánico
  - `mechanic_id` null: liquidar todos los mecánicos elegibles
- Cálculos por mecánico:
  - `total_revenue` = suma de `labor_amount` de facturas SERVICE activas en esa fecha
  - `mechanic_share` = `total_revenue × 0.70` (70%)
  - `shop_share` = `total_revenue × 0.30` (30%)
  - `invoice_count` = cantidad de facturas
- Validaciones:
  - El mecánico debe existir y tener facturas ese día
  - No puede haber liquidación previa para ese mechanic+date (409)
- Las facturas DELIVERY son excluidas (no generan comisión)
- Endpoint: `POST /api/liquidations`
- Status: 201 (Created)

**RF-014: Listar Liquidaciones**
- Filtros opcionales:
  - `mechanic_id`: ID específico
  - `date`: Fecha específica
- Retornar array de liquidaciones con detalles completos
- Endpoint: `GET /api/liquidations`

---

## 4. REQUISITOS NO FUNCIONALES

### 4.1 Rendimiento
- **Tiempo de respuesta API:** < 200ms P95 en carga normal
- **Throughput:** Mínimo 100 solicitudes por segundo
- **Índices:** Trigram index en `invoice_item.description` para búsquedas rápidas
- **Paginación:** No requerida en MVP

### 4.2 Confiabilidad
- **Disponibilidad:** 99.9% en horario operativo (8am-8pm)
- **RTO (Recovery Time Objective):** < 1 hora
- **RPO (Recovery Point Objective):** < 1 hora
- **Backup:** Diario, retenido 30 días

### 4.3 Escalabilidad
- **Usuarios concurrentes:** 5 máximo (aplicación .NET MAUI única)
- **Datos:** Base de datos PostgreSQL local, sin sharding requerido

### 4.4 Seguridad
- **Red:** HTTP únicamente (red privada LAN)
- **Autenticación:** No requerida en MVP
- **Validación:** Todas las entradas deben validarse
- **Inyección SQL:** Usar prepared statements (JPA automáticamente)
- **Integridad:** Transacciones ACID para operaciones críticas

### 4.5 Mantenibilidad
- **Lenguaje:** Java 25
- **Framework:** Spring Boot 4.0.3
- **ORM:** Spring Data JPA / Hibernate 6
- **Base de datos:** PostgreSQL 18
- **Build:** Gradle
- **Código:** Clean Architecture (separación clara de capas)

### 4.6 Usabilidad
- **Respuestas:** Envelope estándar (ApiResponse<T> y ApiErrorResponse)
- **Errores:** Códigos estandarizados, mensajes descriptivos
- **Fechas:** Formato ISO 8601 (YYYY-MM-DD y YYYY-MM-DDTHH:mm:ss)
- **Dinero:** BigDecimal con 2 decimales
- **Charset:** UTF-8

### 4.7 Complemento
- **Análisis:** Metabase OSS conectada directamente a PostgreSQL (no a API)
- **Integraciones:** Ninguna requerida en MVP

---

## 5. ESPECIFICACIONES TÉCNICAS

### 5.1 Stack Tecnológico
| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| Lenguaje | Java | 25 |
| Framework | Spring Boot | 4.0.3 |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Base de datos | PostgreSQL | 18 |
| Build | Gradle | 9.3+ |
| Testing | JUnit 5, Mockito | Latest |

### 5.2 Patrones Arquitectónicos
**Clean Architecture (Hexagonal)**
```
adapter/controller/          → Controladores, DTOs
domain/usecase/              → Lógica de negocio pura
domain/model/                → Entidades de negocio
domain/port/                 → Interfaces (repositorios)
infrastructure/config/       → Configuración Spring, beans
infrastructure/application/  → Facades transaccionales
infrastructure/repository/   → Implementaciones JPA
infrastructure/entity/       → Entidades JPA
infrastructure/exception/    → Excepciones de negocio
```

### 5.3 Principios de Codificación
1. **Responsabilidad única:** Cada clase tiene una razón para cambiar
2. **Inversión de dependencias:** Inyectar componentes, no crear instancias
3. **Transaccionalidad:** `@Transactional` en operaciones de escritura
4. **Validación:** En capas de servicio y DTOs, nunca confiar en cliente
5. **Cálculos críticos:** Siempre en backend, nunca confiar en frontend

### 5.4 Convenciones
- **Nomenclatura DB:** snake_case para columnas, PK = table_name + _id
- **Nomenclatura Java:** camelCase para clases y métodos
- **String normalization:** trim() y toUpperCase() para plate, trim() para modelo/descripción
- **API responses:** Siempre usar ApiResponse<T> para éxito, ApiErrorResponse para errores

---

## 6. MODELO DE DATOS

### 6.1 Entidad: Mechanic (Mecánico)
```sql
CREATE TABLE mechanic (
    mechanic_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);
```

**Restricciones:**
- `name` debe ser único por negocio lógico, pero BD no lo fuerza (responsabilidad de app)

### 6.2 Entidad: Vehicle (Vehículo)
```sql
CREATE TABLE vehicle (
    vehicle_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plate VARCHAR(20) NOT NULL UNIQUE,
    model VARCHAR(100) NOT NULL
);
```

**Restricciones:**
- `plate` es único e inmutable (una vez creado no cambia)

### 6.3 Entidad: Invoice (Factura)
```sql
CREATE TABLE invoice (
    invoice_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_type invoice_type_enum NOT NULL,
    mechanic_id BIGINT REFERENCES mechanic(mechanic_id) NULL,
    vehicle_id BIGINT REFERENCES vehicle(vehicle_id) NULL,
    buyer_name VARCHAR(150) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    labor_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_cancelled BOOLEAN NOT NULL DEFAULT false,
    CHECK (
        (invoice_type = 'SERVICE' AND mechanic_id IS NOT NULL AND vehicle_id IS NOT NULL AND buyer_name IS NULL) OR
        (invoice_type = 'DELIVERY' AND mechanic_id IS NULL AND vehicle_id IS NULL AND buyer_name IS NOT NULL)
    )
);
```

**Restricciones:**
- CHECK constraint fuerza validación de tipo (SERVICE vs DELIVERY)

### 6.4 Entidad: InvoiceItem (Línea de Factura)
```sql
CREATE TABLE invoice_item (
    invoice_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoice(invoice_id),
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(10,2) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_invoice_item_description_gin ON invoice_item
    USING GIN(description gin_trgm_ops);
```

**Restricciones:**
- `subtotal` es calculado y almacenado por el backend
- Índice trigram en `description` para búsquedas rápidas (autocomplete)

### 6.5 Entidad: DailyLiquidation (Liquidación Diaria)
```sql
CREATE TABLE daily_liquidation (
    daily_liquidation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mechanic_id BIGINT NOT NULL REFERENCES mechanic(mechanic_id),
    date DATE NOT NULL,
    total_revenue NUMERIC(10,2) NOT NULL,
    mechanic_share NUMERIC(10,2) NOT NULL,
    shop_share NUMERIC(10,2) NOT NULL,
    invoice_count INTEGER NOT NULL,
    UNIQUE(mechanic_id, date)
);
```

**Restricciones:**
- `UNIQUE(mechanic_id, date)` previene liquidaciones dobles

### 6.6 ENUM: InvoiceType
```sql
CREATE TYPE invoice_type_enum AS ENUM ('SERVICE', 'DELIVERY');
```

---

## 7. REGLAS DE NEGOCIO

### 7.1 Reglas de Factura de Servicio
- **Requerimientos Obligatorios:** Mecánico, Vehículo, Ítems
- **Monto de labor:** >= 0, calculado por usuario final
- **Cálculo de total:** suma(ítems subtotal) + labor_amount
- **Vehículo:** crear si no existe (placa única), actualizar modelo si existe

### 7.2 Reglas de Factura de Entrega
- **Requerimientos Obligatorios:** Nombre comprador, Ítems
- **Monto de labor:** siempre 0 (ignorar valor de request)
- **Cálculo de total:** suma(ítems subtotal)
- **Sin mecánico ni vehículo**

### 7.3 Reglas de Cancelación
- **Acción:** Marcar como cancelada (`is_cancelled = true`)
- **Irreversible:** Una vez cancelada, no puede revertirse
- **Auditoría:** El registro debe conservarse para historial
- **Efecto:** No incluir en listados por defecto (filtro `cancelled = false`)

### 7.4 Reglas de Liquidación Diaria
- **Elegibilidad:** Solo facturas SERVICE (`invoice_type = SERVICE`)
- **Activas:** Solo facturas no canceladas (`is_cancelled = false`)
- **Periodo:** Todas en la misma fecha
- **Cálculos:**
  - `total_revenue`: suma de `labor_amount`
  - `mechanic_share`: 70% del total_revenue
  - `shop_share`: 30% del total_revenue
  - `invoice_count`: cantidad de facturas
- **Comisión:** Las facturas DELIVERY no generan comisión
- **Prevención:** No permitir liquidaciones dobles (mechanic_id + date)

### 7.5 Reglas de Vehículo
- **Placa:** Única, inmutable, normalizada a mayúsculas
- **Modelo:** Mutable (permite correcciones)
- **Creación:** Automática al crear factura SERVICE si no existe
- **Actualización:** Automática si ya existe (misma transacción)

---

## 8. CONTRATO DE API REST

### 8.1 Configuración General
| Propiedad | Valor |
|-----------|-------|
| Base URL (dev) | `http://localhost:8080/api` |
| Base URL (prod) | `http://[SERVER_IP]:8080/api` |
| Content-Type | `application/json` |
| Charset | UTF-8 |
| Protocolo | HTTP (no HTTPS) |

### 8.2 Formatos de Datos
| Tipo | Formato | Ejemplo |
|------|---------|---------|
| Fecha | YYYY-MM-DD | 2026-01-28 |
| Timestamp | YYYY-MM-DDTHH:mm:ss | 2026-01-28T09:45:00 |
| Dinero | BigDecimal (2 decimales) | 209100.00 |
| Booleano | true/false | true |

### 8.3 Respuesta Exitosa (200, 201)
```json
{
    "data": {
        "id": 1,
        "name": "Jose",
        "is_active": true
    },
    "timestamp": "2026-03-27T22:45:00"
}
```

### 8.4 Respuesta de Error
```json
{
    "code": "MECHANIC_NOT_FOUND",
    "message": "Mechanic with ID 99 does not exist",
    "timestamp": "2026-03-27T22:45:00"
}
```

### 8.5 Códigos de Error Estándar
| Código | HTTP | Significado |
|--------|------|------------|
| VALIDATION_ERROR | 400 | Validación de datos fallida |
| VEHICLE_NOT_FOUND | 404 | Vehículo no existe (condición esperada) |
| MECHANIC_NOT_FOUND | 404 | Mecánico no existe |
| INVOICE_NOT_FOUND | 404 | Factura no existe |
| INVOICE_ALREADY_CANCELLED | 409 | Factura ya está cancelada |
| LIQUIDATION_ALREADY_EXISTS | 409 | Liquidación ya existe para esa fecha |
| INTERNAL_ERROR | 500 | Error interno del servidor |

### 8.6 Resumen de Endpoints
| Método | Ruta | Descripción | HTTP |
|--------|------|-------------|------|
| GET | `/mechanics` | Listar mecánicos | 200 |
| GET | `/mechanics/{id}` | Obtener mecánico | 200 / 404 |
| POST | `/mechanics` | Crear mecánico | 201 / 400 |
| PATCH | `/mechanics/{id}/status` | Cambiar estatus | 200 / 404 |
| GET | `/vehicles/plate/{plate}` | Buscar vehículo | 200 / 404 |
| GET | `/invoices` | Listar facturas | 200 |
| GET | `/invoices/{id}` | Obtener factura | 200 / 404 |
| POST | `/invoices/service` | Crear factura servicio | 201 / 400 / 404 |
| POST | `/invoices/delivery` | Crear factura entrega | 201 / 400 |
| PATCH | `/invoices/{id}/cancel` | Cancelar factura | 200 / 404 / 409 |
| GET | `/invoice-items/suggestions` | Autosugencias | 200 |
| GET | `/liquidations` | Listar liquidaciones | 200 |
| POST | `/liquidations` | Crear liquidación | 201 / 404 / 409 |

---

## 9. CRITERIOS DE ACEPTACIÓN

### 9.1 Funcionalidad
- [ ] Todas las operaciones CRUD funcionan correctamente
- [ ] Validaciones se ejecutan en tiempo de solicitud
- [ ] Cálculos automáticos son precisos (subtotales, totales, comisiones)
- [ ] Las transacciones son atómicas (invoice + items se crean juntos)
- [ ] Los filtros funcionan correctamente en listados

### 9.2 Integridad de Datos
- [ ] Las restricciones de CHECK se aplican correctamente
- [ ] Las restricciones UNIQUE previenen duplicados
- [ ] Las claves foráneas se mantienen íntegras
- [ ] Los datos se normalizan correctamente (trim, uppercase)
- [ ] No hay pérdida de precisión en cálculos monetarios

### 9.3 Excepciones y Errores
- [ ] Se retornan los códigos HTTP correctos
- [ ] Los códigos de error de negocio son precisos
- [ ] No se revelan stack traces en respuestas
- [ ] Los mensajes de error son descriptivos

### 9.4 Rendimiento
- [ ] Las respuestas se retornan en < 200ms en casos normales
- [ ] Las búsquedas con filtros son rápidas (< 100ms)
- [ ] Las autosugencias retornan resultados en < 50ms
- [ ] No hay fugas de memoria en conexiones DB

### 9.5 Seguridad
- [ ] No hay inyección SQL (validación de entradas)
- [ ] Las operaciones sensibles usan transacciones
- [ ] No se registran credenciales ni datos sensibles
- [ ] Los errores no revelan detalles de infraestructura

### 9.6 Documentación
- [ ] El código incluye comentarios en puntos clave
- [ ] Existe documentación de uso de API
- [ ] Los errores tienen ejemplos claros
- [ ] La arquitectura está documentada

---

## 10. RESTRICCIONES Y CONSIDERACIONES IMPORTANTES

### 10.1 Técnicas
- **Red privada:** No hay HTTPS (red privada LAN)
- **Una aplicación cliente:** Solo .NET MAUI, no múltiples clientes
- **Base de datos local:** PostgreSQL en contenedor Docker, no remota
- **Sin autenticación:** MVP no requiere sistema de seguridad

### 10.2 Operacionales
- **Horario de operación:** 8am - 8pm (operación diaria)
- **Mantenimiento:** Realizar durante horas no operativas
- **Backup:** Diario, retenido 30 días
- **Monitoreo:** Básico, sin dashboards (monitores locales)

### 10.3 De Datos
- **Retención:** Las facturas canceladas se conservan (auditoría)
- **Privacidad:** Datos de clientes en red privada (GDPR/CCPA no aplica)
- **Historial:** No hay "borrado lógico" (soft delete) para facturas

### 10.4 Financieras
- **Precisión:** Todas las operaciones monetarias en BigDecimal
- **Rondeo:** HALF_UP (0.5 redondea hacia arriba)
- **Formato:** 2 decimales siempre (XXX.00)
- **Moneda:** Peso colombiano (COP), aunque BD no almacena símbolo

### 10.5 No Hacer
❌ **Nunca:**
- Añadir autenticación sin aprobación explícita
- Usar HTTP en entorno público (aunque sea MVP)
- Confiar en valores numéricos del cliente (siempre recalcular)
- Eliminar facturas (solo marcar como canceladas)
- Crear índices sin análisis de rendimiento
- Usar características beta de Spring sin validación

✅ **Siempre:**
- Validar entradas en servidor
- Usar transacciones para operaciones de escritura
- Mantener auditoría de cambios
- Documentar cambios de requisitos
- Ejecutar suite completa de tests