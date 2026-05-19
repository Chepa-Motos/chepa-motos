# Guía de instalación — Chepa Motos

Esta guía explica, paso a paso, cómo levantar el proyecto **desde cero** en una
máquina nueva. Cubre los tres componentes:

| Componente | Descripción | Puerto |
|---|---|---|
| **PostgreSQL 18** | Base de datos | `localhost:5432` |
| **chepa-api** | API REST en Spring Boot (Java 25) | `localhost:8080` |
| **Metabase** | Dashboards analíticos (opcional) | `localhost:3000` |
| **chepa-desktop** | Cliente .NET MAUI (Windows) | — |
| **Bruno collection** | Colección para probar la API manualmente | — |

Al terminar la guía vas a poder:

1. Levantar el backend completo (PostgreSQL + API + Metabase) con un solo comando.
2. Iniciar sesión con el usuario gerente creado automáticamente.
3. Probar todos los endpoints con la colección de Bruno.
4. Ejecutar la aplicación de escritorio y usarla contra la API local.

---

## 1. Prerrequisitos

Instala lo siguiente **antes** de continuar:

| Software | Versión mínima | Para qué se usa | Verificar instalación |
|---|---|---|---|
| **Git** | cualquier reciente | Clonar el repo | `git --version` |
| **Docker Desktop** | última estable | PostgreSQL + API + Metabase | `docker --version` |
| **.NET SDK** | **10.0** | Compilar y ejecutar el cliente desktop | `dotnet --version` |
| **MAUI workload** | versión que acompañe a .NET 10 | Cliente desktop | `dotnet workload list` |
| **Bruno** *(opcional)* | última estable | Probar la API sin abrir el cliente | https://www.usebruno.com |

### 1.1 Instalar el MAUI workload

Una vez instalado el .NET 10 SDK, ejecuta (PowerShell con permisos):

```powershell
dotnet workload install maui
```

Verifica con `dotnet workload list` que aparezcan `maui-windows`, `maui-android`, etc.

> En **Linux/macOS** no podrás ejecutar el cliente desktop completo (depende de
> `net10.0-windows10.0.19041.0`). El backend sí funciona en cualquier sistema con
> Docker.

---

## 2. Clonar el repositorio

```bash
git clone https://github.com/Chepa-Motos/chepa-motos.git
cd chepa-motos
```

A partir de aquí, todos los comandos se ejecutan desde la **raíz del repositorio**
salvo que se indique lo contrario.

---

## 3. Configurar el archivo `.env`

Docker Compose lee variables sensibles desde `.env` (no versionado).

### 3.1 Crear el archivo

```bash
cp .env.example .env
```

### 3.2 Editar `.env` y completar los valores

Abre `.env` con cualquier editor y rellena así:

```dotenv
# Credenciales de PostgreSQL
DB_USER=postgres
DB_PASSWORD=postgres_password_segura

# Secreto para firmar los JWT (mínimo 32 caracteres)
JWT_SECRET=cambia-esto-por-un-secreto-real-de-32-caracteres-minimo

# Bootstrap del primer usuario gerente (ver sección 4)
ADMIN_USERNAME=gerente
ADMIN_PASSWORD=password
```

**Importante sobre cada variable**:

- `DB_USER` / `DB_PASSWORD`: cualquier valor que elijas. PostgreSQL los usa al
  inicializar el contenedor.
- `JWT_SECRET`: cadena de **al menos 32 caracteres**. La API lo valida al
  arrancar y lanza error si es más corto. Puedes generar uno seguro con:

  ```bash
  # macOS / Linux
  openssl rand -base64 48

  # Windows PowerShell
  [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
  ```

- `ADMIN_USERNAME` / `ADMIN_PASSWORD`: usuario gerente que la API crea
  automáticamente al arrancar **si no existe**. Detalle en la sección 4.

> **Nunca subas el `.env` a Git.** Ya está incluido en `.gitignore`.

---

## 4. Bootstrap del usuario gerente (admin)

La API tiene un mecanismo de **bootstrap one-shot**: si las variables
`ADMIN_USERNAME` y `ADMIN_PASSWORD` están definidas en el entorno **y** ese
usuario no existe en la base de datos, lo crea automáticamente al arrancar con
rol `GERENTE`.

### 4.1 ¿Cuándo se ejecuta?

- **Primer arranque del stack**: el usuario aún no existe → se crea.
- **Arranques subsiguientes**: el usuario ya existe → la API lo detecta y
  **no hace nada** (no sobrescribe la contraseña).

### 4.2 Recomendación de seguridad

Una vez verificado que puedes iniciar sesión con el gerente creado, edita el
`.env` y **vacía** `ADMIN_PASSWORD`:

```dotenv
ADMIN_USERNAME=gerente
ADMIN_PASSWORD=
```

Después reinicia el stack:

```bash
docker compose up -d
```

Así la contraseña no queda persistida en el archivo.

> Si te equivocaste con la contraseña al primer arranque, borra el volumen de
> Postgres (sección 11.2) y vuelve a empezar.

---

## 5. Levantar el backend con Docker Compose

Con el `.env` listo, levanta los tres servicios:

```bash
docker compose up --build -d
```

- `--build`: construye la imagen de la API a partir de `chepa-api/Dockerfile`
  (necesario la primera vez y cuando cambia código del backend).
- `-d`: corre en background (detached).

El primer arranque tarda entre **2 y 5 minutos** porque tiene que:

1. Descargar imágenes (`postgres:18`, `metabase/metabase:v0.49.4`).
2. Compilar el JAR de Spring Boot con Gradle.
3. Inicializar PostgreSQL con los scripts SQL de `init/`.

### 5.1 Verificar que los tres servicios estén arriba

```bash
docker compose ps
```

Espera a que las tres líneas estén en estado **`Up`** y `chepa_api` /
`chepa_postgres` en `(healthy)`:

```
NAME              STATUS                  PORTS
chepa_api         Up X minutes (healthy)  0.0.0.0:8080->8080/tcp
chepa_metabase    Up X minutes            0.0.0.0:3000->3000/tcp
chepa_postgres    Up X minutes (healthy)  0.0.0.0:5432->5432/tcp
```

### 5.2 Verificar logs si algo falla

```bash
docker compose logs chepa-api --tail 50
docker compose logs postgres --tail 50
```

Errores comunes y solución en la sección 11.

### 5.3 Inicialización de la base de datos

Al primer arranque, PostgreSQL ejecuta automáticamente los scripts de `init/`
en orden:

- `00_create_metabase_db.sql` — crea la base `metabase` para los dashboards.
- `01_restore_metabase.sql` — restaura los dashboards de analítica de Metabase.
- `02_schema.sql` — crea las tablas de `chepa_motos`: mecánicos, vehículos,
  facturas, ítems, liquidaciones, usuarios, refresh tokens.
- `03_delete_metabase_welcome_screen.sql` — esconde el wizard de bienvenida de Metabase.
- `04_seed.sql` — semilla con datos de ejemplo (mecánicos, vehículos,
  facturas históricas) para que la app no arranque vacía.

> Los scripts solo se ejecutan **si el volumen `postgres_data` está vacío**. Si
> luego cambias un script y necesitas re-ejecutarlo, ver sección 11.2.

---

## 6. Verificar que la API responde

### 6.1 Healthcheck

```bash
curl http://localhost:8080/actuator/health
```

Esperado:

```json
{"status":"UP"}
```

### 6.2 Swagger UI

Abre en el navegador: **http://localhost:8080/swagger-ui.html**

Deberías ver la documentación interactiva de los endpoints.

### 6.3 Spec OpenAPI

- JSON: http://localhost:8080/v3/api-docs
- YAML: http://localhost:8080/v3/api-docs.yaml

---

## 7. Probar la API con Bruno

La carpeta `chepa-api/bruno-chepa-api/` contiene una colección **nativa de
Bruno** con los 16 endpoints de la API.

### 7.1 Instalar Bruno

Descarga e instala desde https://www.usebruno.com (Windows / macOS / Linux).

### 7.2 Abrir la colección

1. Abre Bruno.
2. **Open Collection** → selecciona la carpeta `chepa-api/bruno-chepa-api/`.
3. La colección aparece en el panel izquierdo con los 16 requests.

### 7.3 Importar el environment **Local**

La colección viene con un environment llamado `Local` que contiene las
credenciales y la URL base. Si Bruno no lo detecta automáticamente al abrir la
colección:

1. En Bruno, ve al menú de environments (icono al lado del nombre de la
   colección).
2. **Import Environment**.
3. Selecciona el archivo `chepa-api/bruno-chepa-api/Local.json` (no el `.bru`).
4. Selecciona el environment `Local` en el dropdown.

### 7.4 Sincronizar credenciales con tu `.env`

El environment `Local` viene preconfigurado con:

```
adminUsername = gerente
adminPassword = password
```

> Si en el `.env` definiste un `ADMIN_USERNAME` o `ADMIN_PASSWORD` distintos,
> edita el environment `Local` en Bruno (botón "Edit environment") y ajusta
> esos dos valores. **De lo contrario el login fallará con `INVALID_CREDENTIALS`.**

### 7.5 Ejecutar los requests en orden

Los archivos `.bru` están numerados del 01 al 16. Ejecútalos en orden la
primera vez:

| # | Request | Qué hace |
|---|---|---|
| 01 | Auth Login | Guarda `authToken` y `refreshToken` en el environment |
| 02 | Create Mechanic | Guarda `mechanicId` |
| 03 | Get Mechanic By Id | |
| 04 | List Mechanics | |
| 05 | Create Service Invoice | Guarda `invoiceId` |
| 06 | Lookup Vehicle By Plate | |
| 07 | Create Delivery Invoice | |
| 08 | List Invoices | |
| 09 | Get Invoice By Id | |
| 10 | Cancel Invoice | |
| 11 | Invoice Item Suggestions | |
| 12 | Create Liquidation | |
| 13 | List Liquidations | |
| 14 | Change Mechanic Status | |
| 15 | Auth Refresh | Rota el `authToken` |
| 16 | Auth Logout | |

Los requests **02, 14 y 12** (crear mecánico, cambiar estado, crear
liquidación) requieren rol **GERENTE**. Si el login del paso 01 fue exitoso,
los siguientes ya llevan el `Authorization: Bearer {{authToken}}`
automáticamente.

> Más detalles en `chepa-api/bruno-chepa-api/README.md`.

---

## 8. Compilar y ejecutar el cliente desktop

El cliente vive en `chepa-desktop/ChepaMotos/`.

### 8.1 Restaurar dependencias

```bash
cd chepa-desktop/ChepaMotos
dotnet restore ChepaMotos/ChepaMotos.csproj
```

### 8.2 Compilar para Windows

```bash
dotnet build ChepaMotos/ChepaMotos.csproj -f net10.0-windows10.0.19041.0
```

Si todo va bien, debería terminar con `Build succeeded. 0 Warning(s) 0 Error(s)`.

### 8.3 Ejecutar la aplicación

**Opción A — desde la línea de comandos:**

```bash
dotnet run --project ChepaMotos/ChepaMotos.csproj -f net10.0-windows10.0.19041.0
```

**Opción B — desde un IDE:**

- **Visual Studio 2022/2026**: abre `ChepaMotos.slnx`, selecciona el TFM
  `net10.0-windows10.0.19041.0` y presiona F5.
- **JetBrains Rider**: abre `ChepaMotos.slnx`, configuración `ChepaMotos:
  Windows Machine` y run.

La app abre una ventana con la pantalla de **Login**.

### 8.4 Configuración del cliente

El cliente lee `chepa-desktop/ChepaMotos/ChepaMotos/Resources/Raw/appsettings.json`,
que apunta por defecto a `http://localhost:8080/api`. Si quieres apuntar a otro
servidor sin recompilar, crea un override externo en:

- **Windows**: `%LOCALAPPDATA%\ChepaMotos\appsettings.json`

con el contenido:

```json
{
  "Api": {
    "BaseUrl": "http://192.168.x.x:8080/api",
    "TimeoutSeconds": 15
  },
  "Metabase": {
    "EmbedUrl": "http://192.168.x.x:3000/"
  }
}
```

---

## 9. Smoke test end-to-end

Con el backend levantado y el cliente compilado:

1. **Arranca la app desktop** (sección 8.3).
2. En el **LoginPage**, ingresa las credenciales del `.env`:
   - Usuario: el valor de `ADMIN_USERNAME` (por defecto `gerente`).
   - Contraseña: el valor de `ADMIN_PASSWORD` (por defecto `password`).
3. Click en **Iniciar sesión**.
4. Deberías entrar al `MainLayout`. En el sidebar inferior izquierdo verás tu
   username + el rol `GERENTE` + botón **Salir**.
5. Navega entre las secciones del sidebar: Inicio, Facturas, Liquidaciones,
   Dashboards (Metabase), Mecánicos.
6. Click en **Factura de Servicio** (sidebar inferior). Llena los campos y
   confirma. Deberías ver el toast "Factura de servicio registrada".

### 9.1 Atajos de teclado disponibles

| Atajo | Acción |
|---|---|
| `Ctrl + R` | Recargar vista actual |
| `Ctrl + N` | Abrir Factura de Servicio |
| `Ctrl + Shift + N` | Abrir Factura de Venta |

---

## 10. Tests automáticos (opcional)

El proyecto tiene **dos suites independientes**: una para el backend (JUnit 5 + Testcontainers)
y otra para el cliente desktop (xUnit + Moq).

### 10.1 Tests del backend (`chepa-api`)

Stack: JUnit 5, Spring Boot Test, Spring Security Test, **Testcontainers**
(arranca un PostgreSQL real en Docker durante los tests de infraestructura),
**ArchUnit** (valida que las dependencias entre capas respeten la arquitectura
hexagonal) y **JaCoCo** para coverage.

> **Requisito**: Docker debe estar corriendo (Testcontainers levanta su propio
> Postgres efímero, distinto al del `docker compose`). No necesitas tener el
> stack del proyecto arriba.

**Ejecutar toda la suite:**

```bash
cd chepa-api

# Linux / macOS
./gradlew test

# Windows
gradlew.bat test
```

Esto compila el proyecto si hace falta y corre los **24 archivos de tests**
distribuidos en `src/test/java/com/chepamotos/`:

- `domain/model/` — pruebas de los modelos de dominio.
- `domain/usecase/auth|invoice|liquidation|mechanic|vehicle/` — casos de uso.
- `domain/architecture/` — reglas ArchUnit del dominio.
- `adapter/controller/` — controladores REST.
- `adapter/architecture/` — reglas ArchUnit del adapter.
- `infrastructure/repository|mapper|security|application/` — capa de
  infraestructura (con Testcontainers donde toca DB).

**Ejecutar solo una capa** (más rápido, no levanta Spring/DB completos):

```bash
./gradlew domainTest          # solo dominio (puro, sin Spring ni DB)
./gradlew adapterTest         # solo adapter
./gradlew infrastructureTest  # solo infraestructura
```

**Generar reporte de cobertura JaCoCo:**

```bash
./gradlew test jacocoTestReport
```

El HTML del reporte queda en `chepa-api/build/reports/jacoco/test/html/index.html`.
También existen reportes por capa: `jacocoDomainTestReport`,
`jacocoAdapterTestReport`, `jacocoInfrastructureTestReport`.

**Verificación de umbrales de cobertura** (si quieres que falle el build
cuando la cobertura baja):

```bash
./gradlew jacocoTestCoverageVerification
```

### 10.2 Tests del cliente desktop (`chepa-desktop`)

Stack: xUnit + Moq. Suite de **38 tests unitarios** en
`chepa-desktop/ChepaMotos/ChepaMotos.Tests/` que cubren la capa de servicios
HTTP, auth, JWT parsing y mapeo de DTOs. No requieren Docker ni el backend
levantado (todo va por mocks).

```bash
cd chepa-desktop/ChepaMotos/ChepaMotos.Tests
dotnet test
```


### 10.3 Correr ambas suites en una sola sesión

Desde la raíz del repositorio:

```bash
# Backend
cd chepa-api && ./gradlew test && cd ..

# Cliente desktop
cd chepa-desktop/ChepaMotos/ChepaMotos.Tests && dotnet test && cd ../../..
```

---

## 11. Solución de problemas

### 11.1 La API no arranca: "JWT secret must be at least 32 bytes"

El valor de `JWT_SECRET` en `.env` es muy corto. Genera uno nuevo (sección 3.2)
y reinicia:

```bash
docker compose down
docker compose up -d
```

### 11.2 Recrear la base de datos desde cero

Si necesitas reejecutar los scripts de `init/` (por ejemplo, tras modificar el
schema o cambiar el `ADMIN_PASSWORD` del primer bootstrap):

```bash
docker compose down -v   # -v borra los volúmenes, incluido postgres_data
docker compose up --build -d
```

> **Atención**: esto borra **toda** la información. No lo hagas en producción.

### 11.3 "Login failed: INVALID_CREDENTIALS" desde Bruno o el cliente

- Verifica que `ADMIN_USERNAME` y `ADMIN_PASSWORD` del `.env` coinciden con
  lo que estás enviando.
- Si cambiaste el `.env` después del primer arranque, los cambios **no
  re-bootstrappean** el usuario. Tienes dos opciones:
  - Borrar el volumen (11.2) y rearrancar.
  - Actualizar la contraseña manualmente vía SQL.

### 11.4 El cliente desktop no encuentra la API

- Verifica que `docker compose ps` muestra `chepa-api` en estado `(healthy)`.
- Prueba `curl http://localhost:8080/actuator/health` desde la misma máquina
  donde corre el cliente.
- Si el cliente corre en una máquina distinta al backend (otra LAN), edita
  el override de `appsettings.json` (sección 8.4) y pon la IP del servidor.

### 11.5 Errores al compilar el cliente: "MAUI workload not installed"

```powershell
dotnet workload install maui
dotnet workload update
```

Reinicia el IDE después de instalar workloads.

### 11.6 Logs del cliente desktop

Si el cliente se comporta raro y quieres revisar qué pasó internamente, los
logs viven en:

- **Windows**: `%LOCALAPPDATA%\ChepaMotos\logs\chepa-{yyyyMMdd}.log`

Incluyen cada request HTTP (método, path, status, duración) y eventos de
autenticación. No hay PII ni contraseñas.

### 11.7 Apagar todo limpiamente

```bash
docker compose down
```

Mantiene los volúmenes (puedes volver a `up` sin perder datos). Si quieres
parar también el volumen de Postgres:

```bash
docker compose down -v
```

---

## 12. Referencias rápidas

| Recurso | URL / Ubicación |
|---|---|
| API base | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Spec OpenAPI (YAML) | http://localhost:8080/v3/api-docs.yaml |
| Healthcheck | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432` (base `chepa_motos`) |
| Metabase | http://localhost:3000 |
| Colección Bruno | `chepa-api/bruno-chepa-api/` |
| Environment Bruno | `chepa-api/bruno-chepa-api/environments/Local.bru` |
| `.env` ejemplo | `.env.example` |
| Cliente desktop | `chepa-desktop/ChepaMotos/ChepaMotos.slnx` |
| Tests | `chepa-desktop/ChepaMotos/ChepaMotos.Tests/` |
| README backend | `README.md` |
| Requisitos detallados | `chepa-api/REQUISITOS.md` |

---

## 13. Resumen ultra-corto (para cuando ya conoces la guía)

```bash
# 1. Clonar y entrar
git clone https://github.com/Chepa-Motos/chepa-motos.git
cd chepa-motos

# 2. Configurar .env
cp .env.example .env
# Editar .env y poner JWT_SECRET (32+ chars) y ADMIN_PASSWORD

# 3. Levantar backend
docker compose up --build -d

# 4. Verificar
curl http://localhost:8080/actuator/health

# 5. Compilar y ejecutar cliente (Windows)
cd chepa-desktop/ChepaMotos
dotnet run --project ChepaMotos/ChepaMotos.csproj -f net10.0-windows10.0.19041.0

# 6. Login en la app: gerente / password (o lo que pusiste en .env)
```

Si algo falla, ver sección **11. Solución de problemas**.
