namespace ChepaMotos.Config;

/// <summary>
/// Configuración de la app cargada en arranque desde
/// <c>Resources/Raw/appsettings.json</c> (embebido) con override opcional desde
/// <c>%LOCALAPPDATA%/ChepaMotos/appsettings.json</c> en Windows o
/// <c>~/.local/share/ChepaMotos/appsettings.json</c> en otros sistemas.
///
/// Esto permite que un cliente cambie el endpoint sin recompilar — útil para
/// mover de localhost a un servidor real, o para apuntar a staging sin tener
/// que re-empaquetar la app.
/// </summary>
public interface IAppConfig
{
    /// <summary>BaseUrl de la API de Chepa, ya normalizada para la plataforma actual.</summary>
    string BaseUrl { get; }

    /// <summary>Timeout del HttpClient en segundos.</summary>
    int TimeoutSeconds { get; }

    /// <summary>URL base para embebidos de Metabase (DashboardsView).</summary>
    string MetabaseUrl { get; }
}
