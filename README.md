# Chepa Motos

Sistema de facturación y gestión para taller de motocicletas.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 25 + Spring Boot 4.0.3 |
| Base de datos | PostgreSQL 18 (Docker) |
| Analítica | Metabase OSS (Docker) |
| Desktop | .NET MAUI (Windows) |
| Móvil | .NET MAUI (Android) |

## Estructura del repositorio

```
chepa-motos/
├── docker-compose.yml       # PostgreSQL + API + Metabase
├── init/
│   ├── 01_create_metabase_db.sql
│   ├── 02_schema.sql
│   └── 03_seed.sql
├── chepa-api/               # Spring Boot REST API
│   └── Dockerfile
└── chepa-desktop/           # .NET MAUI desktop + mobile
```

## Inicio rápido

1. Clonar el repositorio
2. Copiar `.env.example` a `.env` y completar las variables
3. (Opcional recomendado en primer arranque) Definir `ADMIN_USERNAME` y `ADMIN_PASSWORD` para crear el primer gerente automáticamente
3. Ejecutar `docker compose up --build -d`
4. Esperar a que PostgreSQL, la API y Metabase queden en estado healthy/ready
5. API disponible en `http://localhost:8080/api`
6. PostgreSQL disponible en `localhost:5432`
7. Metabase disponible en `localhost:3000`

Nota de seguridad:
Si usas bootstrap de gerente en `.env`, elimina `ADMIN_PASSWORD` del archivo una vez creado el usuario inicial.

## Flujo de arranque

El stack se levanta con un solo comando y queda listo para usarse desde cero:

1. PostgreSQL crea la base `chepa_motos` y ejecuta los scripts de `init/`.
2. La API se construye desde `chepa-api/Dockerfile` y conecta a `postgres` por red interna.
3. Metabase arranca conectado al mismo PostgreSQL.

Si cambias los scripts de `init/` o el código de la API, vuelve a ejecutar `docker compose up --build -d`.

## Convención de ramas

- `main` — rama protegida, solo merge via pull request
- `feature/api/nombre` — features del backend
- `feature/desktop/nombre` — features del frontend
- `fix/api/nombre` — correcciones del backend
- `fix/desktop/nombre` — correcciones del frontend
