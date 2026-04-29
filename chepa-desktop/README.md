# chepa-desktop

.NET MAUI — Windows desktop + Android mobile.

## Configuración del endpoint de la API

La app lee su configuración desde `Resources/Raw/appsettings.json`, que viene
embebido en el binario con valores por defecto (apuntando a `localhost:8080`
para desarrollo).

Para apuntar a un servidor distinto **sin recompilar**, coloca un archivo de
override en una de estas rutas:

| Plataforma | Ruta del override |
|---|---|
| Windows | `%LOCALAPPDATA%\ChepaMotos\appsettings.json` |
| Android | `~/.local/share/ChepaMotos/appsettings.json` |

El override **sobreescribe** las claves que defina; las que falten conservan
el valor del JSON embebido. Estructura del archivo:

```json
{
  "Api": {
    "BaseUrl": "https://api.tudominio.com/api",
    "TimeoutSeconds": 30
  },
  "Metabase": {
    "EmbedUrl": "https://metabase.tudominio.com/embed/dashboard/"
  }
}
```

### Notas

- En Android emulator, `localhost` se reescribe automáticamente a `10.0.2.2`
  (loopback al host). No necesitas tocar el JSON para desarrollo.
- Si el JSON embebido o el override están malformados, la app cae a defaults
  hardcoded (`http://localhost:8080/api`) sin romper el arranque.
- `TimeoutSeconds` aplica al HttpClient global. Subir si tu red es lenta o
  si el backend tiene cold starts (Docker recién levantado).

## Build

```bash
# Windows
dotnet build -f net10.0-windows10.0.19041.0

# Android
dotnet build -f net10.0-android
```

## Run en debug

```bash
cd ChepaMotos/ChepaMotos
dotnet run -f net10.0-windows10.0.19041.0
```
