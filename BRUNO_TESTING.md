# Testing con Bruno

Guía para importar la API en Bruno y ejecutar tests contra el entorno local.

## Prerequisitos

- [Bruno](https://www.usebruno.com/) instalado
- Stack corriendo: `docker compose up --build -d`
- API disponible en `http://localhost:8080`

---

## 1. Importar la colección

1. Con la API corriendo, descarga el spec OpenAPI:
   ```
   http://localhost:8080/v3/api-docs.yaml
   ```
2. En Bruno: **Import Collection → OpenAPI v3** → selecciona el archivo descargado.

Bruno crea una carpeta por tag (Autenticación, Mecánicos, Facturas, etc.) con un request por endpoint.

---

## 2. Crear el environment

En la colección → **Environments → + Create Environment** → nombre: `local`.

Agrega estas variables:

| Variable | Valor | Secreto |
|---|---|---|
| `base_url` | `http://localhost:8080` | No |
| `access_token` | *(vacío)* | Sí |
| `refresh_token` | *(vacío)* | Sí |

Activa el environment `local` en la esquina superior derecha.

---

## 3. Configurar auth en la colección

En la raíz de la colección → pestaña **Auth** → tipo **Bearer Token** → valor:

```
{{access_token}}
```

Todos los requests heredan esta configuración automáticamente.

---

## 4. Configurar el script de login

Abre el request `POST /api/auth/login` → pestaña **Tests** → pega:

```js
if (res.status === 200) {
  bru.setEnvVar("access_token", res.body.data.accessToken);
  bru.setEnvVar("refresh_token", res.body.data.refreshToken);
}
```

---

## 5. Configurar el script de refresh

Abre el request `POST /api/auth/refresh` → pestaña **Tests** → pega:

```js
if (res.status === 200) {
  bru.setEnvVar("access_token", res.body.data.accessToken);
  bru.setEnvVar("refresh_token", res.body.data.refreshToken);
}
```

---

## Flujo de testing

### Inicio de sesión

Ejecuta primero el request de login con las credenciales del gerente:

```json
{
  "username": "tu_usuario",
  "password": "tu_contraseña"
}
```

El script guarda `access_token` y `refresh_token` automáticamente. Desde este punto todos los requests funcionan sin configuración adicional.

### Endpoints públicos

No requieren ningún paso previo. Funcionan directamente:

| Endpoint | Descripción |
|---|---|
| `GET /api/mechanics` | Listar mecánicos activos |
| `GET /api/mechanics/{id}` | Obtener mecánico por ID |
| `GET /api/vehicles/plate/{plate}` | Buscar vehículo por placa |
| `GET /api/invoice-items/suggestions` | Sugerencias de ítems |
| `GET /api/invoices` | Listar facturas |
| `GET /api/invoices/{id}` | Obtener factura por ID |
| `POST /api/invoices/service` | Crear factura de servicio |
| `POST /api/invoices/delivery` | Crear factura de entrega |
| `PATCH /api/invoices/{id}/cancel` | Cancelar factura |
| `GET /api/liquidations` | Listar liquidaciones |

### Endpoints protegidos (requieren rol GERENTE)

Requieren haber ejecutado el login previamente. El token se envía automáticamente:

| Endpoint | Descripción |
|---|---|
| `POST /api/mechanics` | Crear mecánico |
| `PATCH /api/mechanics/{id}/status` | Activar/desactivar mecánico |
| `POST /api/liquidations` | Ejecutar liquidación diaria |

Si el token expiró (~15 min), ejecuta `POST /api/auth/refresh` para renovarlo.

### Logout

Ejecuta `POST /api/auth/logout` con el body:

```json
{
  "refreshToken": "{{refresh_token}}"
}
```

El refresh token queda invalidado en el servidor. Para volver a usar endpoints protegidos hay que hacer login de nuevo.

---

## Referencia de errores comunes

| Código | Causa | Solución |
|---|---|---|
| `401 AUTH_REQUIRED` | Sin token o token inválido | Ejecutar login |
| `401 SESSION_EXPIRED` | Refresh token expirado | Ejecutar login de nuevo |
| `401 INVALID_CREDENTIALS` | Usuario o contraseña incorrectos | Verificar credenciales |
| `403 FORBIDDEN` | Usuario sin rol GERENTE | Usar cuenta de gerente |
| `404 MECHANIC_NOT_FOUND` | ID de mecánico inexistente | Verificar el ID |
| `409 LIQUIDATION_ALREADY_EXISTS` | Ya existe liquidación para esa fecha | Consultar con GET /liquidations |
