namespace ChepaMotos.Services.Api;

/// <summary>
/// Nombres de <see cref="IHttpClientFactory"/> registrados en DI.
/// Centralizado aquí (no en <c>MauiProgram</c>) para que servicios y tests
/// puedan usarlo sin acoplarse al bootstrap de la app.
/// </summary>
public static class ApiClientNames
{
    /// <summary>Cliente HTTP contra <c>chepa-api</c>. Configurado en MauiProgram.</summary>
    public const string ChepaApi = "chepa-api";
}
