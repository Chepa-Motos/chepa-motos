using System.Text.Json;

namespace ChepaMotos.Config;

/// <summary>
/// Implementación de <see cref="IAppConfig"/>. Use <see cref="Load"/> en arranque
/// para construir la instancia leyendo del JSON embebido y aplicando override
/// externo si existe.
/// </summary>
public sealed class AppConfig : IAppConfig
{
    public string BaseUrl { get; }
    public int TimeoutSeconds { get; }
    public string MetabaseUrl { get; }

    private AppConfig(string baseUrl, int timeoutSeconds, string metabaseUrl)
    {
        BaseUrl = NormalizeForPlatform(baseUrl);
        TimeoutSeconds = timeoutSeconds;
        MetabaseUrl = NormalizeForPlatform(metabaseUrl);
    }

    /// <summary>
    /// Carga la configuración aplicando, en orden: defaults → JSON embebido →
    /// JSON externo opcional. Cada nivel sobrescribe los anteriores. Si el JSON
    /// embebido no se encuentra o tiene errores de parseo, se usan los defaults
    /// (localhost) y la app sigue arrancando.
    /// </summary>
    public static IAppConfig Load()
    {
        // Defaults — sirven como fallback si todo falla.
        var baseUrl = "http://localhost:8080/api";
        var timeoutSeconds = 15;
        var metabaseUrl = "http://localhost:3000/embed/dashboard/";

        var sources = new[]
        {
            ReadEmbeddedAsset("appsettings.json"),
            ReadExternalConfig(),
        };

        foreach (var json in sources)
        {
            if (string.IsNullOrWhiteSpace(json))
                continue;

            try
            {
                using var doc = JsonDocument.Parse(json);
                var root = doc.RootElement;

                if (root.TryGetProperty("Api", out var api))
                {
                    if (api.TryGetProperty("BaseUrl", out var b) && b.ValueKind == JsonValueKind.String)
                    {
                        var v = b.GetString();
                        if (!string.IsNullOrWhiteSpace(v)) baseUrl = v;
                    }
                    if (api.TryGetProperty("TimeoutSeconds", out var t) && t.ValueKind == JsonValueKind.Number)
                    {
                        if (t.TryGetInt32(out var n) && n > 0) timeoutSeconds = n;
                    }
                }
                if (root.TryGetProperty("Metabase", out var mb))
                {
                    if (mb.TryGetProperty("EmbedUrl", out var e) && e.ValueKind == JsonValueKind.String)
                    {
                        var v = e.GetString();
                        if (!string.IsNullOrWhiteSpace(v)) metabaseUrl = v;
                    }
                }
            }
            catch (JsonException)
            {
                // JSON malformado → ignoramos esta fuente y seguimos con la siguiente.
            }
        }

        return new AppConfig(baseUrl, timeoutSeconds, metabaseUrl);
    }

    /// <summary>
    /// Convierte URLs hacia <c>localhost</c> cuando corremos en el emulador de
    /// Android, donde 127.0.0.1 apunta al propio dispositivo emulado y no al host.
    /// </summary>
    private static string NormalizeForPlatform(string url)
    {
        if (DeviceInfo.Platform != DevicePlatform.Android)
            return url;

        return url
            .Replace("://localhost", "://10.0.2.2", StringComparison.OrdinalIgnoreCase)
            .Replace("://127.0.0.1", "://10.0.2.2", StringComparison.OrdinalIgnoreCase);
    }

    private static string? ReadEmbeddedAsset(string filename)
    {
        try
        {
            using var stream = FileSystem.OpenAppPackageFileAsync(filename).GetAwaiter().GetResult();
            using var reader = new StreamReader(stream);
            return reader.ReadToEnd();
        }
        catch
        {
            return null;
        }
    }

    private static string? ReadExternalConfig()
    {
        try
        {
            var dir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "ChepaMotos");
            var path = Path.Combine(dir, "appsettings.json");
            if (!File.Exists(path))
                return null;
            return File.ReadAllText(path);
        }
        catch
        {
            return null;
        }
    }
}
