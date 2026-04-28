using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using ChepaMotos.Models;
using ChepaMotos.Services.Auth;

namespace ChepaMotos.Services.Api;

public sealed class ApiClient : IApiClient
{
    private static readonly JsonSerializerOptions JsonOptions = new();

    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ITokenStore _tokenStore;
    private readonly IAuthService _authService;
    private readonly IAuthState _authState;
    private readonly SemaphoreSlim _refreshLock = new(1, 1);

    public ApiClient(
        IHttpClientFactory httpClientFactory,
        ITokenStore tokenStore,
        IAuthService authService,
        IAuthState authState)
    {
        _httpClientFactory = httpClientFactory;
        _tokenStore = tokenStore;
        _authService = authService;
        _authState = authState;
    }

    public Task<T> GetAsync<T>(string path, IDictionary<string, string?>? query = null, CancellationToken ct = default)
        => SendAsync<T>(HttpMethod.Get, BuildUrl(path, query), bodyJson: null, ct);

    public Task<TResponse> PostAsync<TResponse, TRequest>(string path, TRequest body, CancellationToken ct = default)
        => SendAsync<TResponse>(HttpMethod.Post, path, JsonSerializer.Serialize(body, JsonOptions), ct);

    public Task<TResponse> PostAsync<TResponse>(string path, CancellationToken ct = default)
        => SendAsync<TResponse>(HttpMethod.Post, path, bodyJson: null, ct);

    public Task<TResponse> PatchAsync<TResponse, TRequest>(string path, TRequest body, CancellationToken ct = default)
        => SendAsync<TResponse>(HttpMethod.Patch, path, JsonSerializer.Serialize(body, JsonOptions), ct);

    public Task<TResponse> PatchAsync<TResponse>(string path, CancellationToken ct = default)
        => SendAsync<TResponse>(HttpMethod.Patch, path, bodyJson: null, ct);

    private async Task<T> SendAsync<T>(HttpMethod method, string url, string? bodyJson, CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient(MauiProgram.ApiHttpClientName);

        var attempt = await ExecuteAsync(client, method, url, bodyJson, ct);

        if (attempt.IsSuccess)
            return Deserialize<T>(attempt.Body, attempt.StatusCode);

        // Refresh transparente: solo si el backend devuelve 401 con AUTH_REQUIRED.
        // Para INVALID_CREDENTIALS o SESSION_EXPIRED no hay nada que refrescar.
        if (attempt.StatusCode == HttpStatusCode.Unauthorized
            && string.Equals(attempt.ErrorCode, ApiErrorCodes.AuthRequired, StringComparison.Ordinal)
            && await TryRefreshAsync(ct))
        {
            attempt = await ExecuteAsync(client, method, url, bodyJson, ct);
            if (attempt.IsSuccess)
                return Deserialize<T>(attempt.Body, attempt.StatusCode);
        }

        throw new ApiException(
            attempt.ErrorCode ?? ApiErrorCodes.InternalError,
            attempt.ErrorMessage ?? $"Error HTTP {(int)attempt.StatusCode}",
            (int)attempt.StatusCode);
    }

    private async Task<HttpAttempt> ExecuteAsync(
        HttpClient client,
        HttpMethod method,
        string url,
        string? bodyJson,
        CancellationToken ct)
    {
        using var request = new HttpRequestMessage(method, url);
        if (bodyJson is not null)
            request.Content = new StringContent(bodyJson, Encoding.UTF8, "application/json");

        var accessToken = await _tokenStore.GetAccessTokenAsync();
        if (!string.IsNullOrEmpty(accessToken))
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);

        using var response = await client.SendAsync(request, ct);
        var bodyStr = await response.Content.ReadAsStringAsync(ct);

        if (response.IsSuccessStatusCode)
            return new HttpAttempt(true, response.StatusCode, bodyStr, ErrorCode: null, ErrorMessage: null);

        string? code = null;
        string? message = null;
        if (!string.IsNullOrWhiteSpace(bodyStr))
        {
            try
            {
                var error = JsonSerializer.Deserialize<ApiErrorResponse>(bodyStr, JsonOptions);
                code = error?.Code;
                message = error?.Message;
            }
            catch (JsonException) { /* respuesta no-JSON, caemos a fallback */ }
        }

        return new HttpAttempt(false, response.StatusCode, bodyStr, code, message);
    }

    /// <summary>
    /// Refresh con candado para evitar dos refreshes concurrentes cuando varias
    /// requests reciben 401 al mismo tiempo. Si el refresh falla con error de la
    /// API, propaga <see cref="IAuthState.RaiseSessionExpired"/>; si falla por red
    /// devuelve <c>false</c> sin tocar el estado (la request original fallará con
    /// su propio error).
    /// </summary>
    private async Task<bool> TryRefreshAsync(CancellationToken ct)
    {
        var refreshTokenBeforeWait = await _tokenStore.GetRefreshTokenAsync();
        if (string.IsNullOrEmpty(refreshTokenBeforeWait))
        {
            _authState.RaiseSessionExpired();
            return false;
        }

        await _refreshLock.WaitAsync(ct);
        try
        {
            // Otra request pudo haber refrescado mientras esperábamos el lock.
            var currentRefresh = await _tokenStore.GetRefreshTokenAsync();
            if (!string.IsNullOrEmpty(currentRefresh) && currentRefresh != refreshTokenBeforeWait)
                return true;

            try
            {
                await _authService.RefreshAsync(currentRefresh ?? refreshTokenBeforeWait, ct);
                return true;
            }
            catch (ApiException)
            {
                _authState.RaiseSessionExpired();
                return false;
            }
            catch (HttpRequestException)
            {
                return false;
            }
            catch (TaskCanceledException)
            {
                return false;
            }
        }
        finally
        {
            _refreshLock.Release();
        }
    }

    private static T Deserialize<T>(string body, HttpStatusCode status)
    {
        if (string.IsNullOrWhiteSpace(body))
            throw new ApiException(ApiErrorCodes.InternalError, "Respuesta vacía del servidor", (int)status);

        try
        {
            var envelope = JsonSerializer.Deserialize<ApiResponse<T>>(body, JsonOptions);
            if (envelope is null)
                throw new ApiException(ApiErrorCodes.InternalError, "Respuesta inválida del servidor", (int)status);
            return envelope.Data!;
        }
        catch (JsonException ex)
        {
            throw new ApiException(ApiErrorCodes.InternalError, $"Respuesta inválida del servidor: {ex.Message}", (int)status);
        }
    }

    private static string BuildUrl(string path, IDictionary<string, string?>? query)
    {
        if (query is null || query.Count == 0)
            return path;

        var sb = new StringBuilder(path);
        var first = !path.Contains('?');
        foreach (var kvp in query)
        {
            if (kvp.Value is null) continue;
            sb.Append(first ? '?' : '&');
            sb.Append(Uri.EscapeDataString(kvp.Key));
            sb.Append('=');
            sb.Append(Uri.EscapeDataString(kvp.Value));
            first = false;
        }
        return sb.ToString();
    }

    private readonly record struct HttpAttempt(
        bool IsSuccess,
        HttpStatusCode StatusCode,
        string Body,
        string? ErrorCode,
        string? ErrorMessage);
}
