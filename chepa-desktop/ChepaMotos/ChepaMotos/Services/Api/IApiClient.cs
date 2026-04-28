using ChepaMotos.Models;

namespace ChepaMotos.Services.Api;

/// <summary>
/// Cliente HTTP de alto nivel contra la API de Chepa. Inyecta automáticamente
/// el header <c>Authorization</c>, deserializa el envelope <see cref="ApiResponse{T}"/>
/// y traduce errores 4xx/5xx a <see cref="ApiException"/>.
///
/// Si el backend responde 401 con <c>code = AUTH_REQUIRED</c>, intenta un refresh
/// transparente y reintenta la request UNA vez. Si el refresh falla, dispara
/// <see cref="Auth.IAuthState.SessionExpired"/> y propaga la excepción original.
/// </summary>
public interface IApiClient
{
    Task<T> GetAsync<T>(
        string path,
        IDictionary<string, string?>? query = null,
        CancellationToken ct = default);

    Task<TResponse> PostAsync<TResponse, TRequest>(
        string path,
        TRequest body,
        CancellationToken ct = default);

    Task<TResponse> PostAsync<TResponse>(
        string path,
        CancellationToken ct = default);

    Task<TResponse> PatchAsync<TResponse, TRequest>(
        string path,
        TRequest body,
        CancellationToken ct = default);

    Task<TResponse> PatchAsync<TResponse>(
        string path,
        CancellationToken ct = default);
}
