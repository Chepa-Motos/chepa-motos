# Chepa Motos

Sistema de facturación y gestión para taller de motocicletas.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 25 + Spring Boot 4.0.3 |
| Base de datos | PostgreSQL 16 (Docker) |
| Analítica | Metabase OSS (Docker) |
| Desktop | .NET MAUI (Windows) |
| Móvil | .NET MAUI (Android) |

## Estructura del repositorio

```
chepa-motos/
├── docker-compose.yml       # PostgreSQL + Metabase
├── init/
│   └── 01_create_metabase_db.sql
├── chepa-api/               # Spring Boot REST API
└── chepa-desktop/           # .NET MAUI desktop + mobile
```

## Inicio rápido

1. Clonar el repositorio
2. Copiar `.env.example` a `.env` y completar las variables
3. Ejecutar `docker compose up -d`
4. PostgreSQL disponible en `localhost:5432`
5. Metabase disponible en `localhost:3000`

## Convención de ramas

- `main` — rama protegida, solo merge via pull request
- `feature/api/nombre` — features del backend
- `feature/desktop/nombre` — features del frontend
- `fix/api/nombre` — correcciones del backend
- `fix/desktop/nombre` — correcciones del frontend
