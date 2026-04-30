using System.Diagnostics;
using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using ChepaMotos.Models;
using ChepaMotos.Services.Auth;
using Microsoft.Extensions.Logging;

namespace ChepaMotos.Services.Api;

public sealed class ApiClient : IApiClient
{
    private static readonly JsonSerializerOptions JsonOptions = new();

    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ITokenStore _tokenStore;
    private readonly IAuthService _authService;
    private readonly IAuthState _authState;
    private readonly ILogger<ApiClient> _logger;

    public ApiClient(
        IHttpClientFactory httpClientFactory,
        ITokenStore tokenStore,
        IAuthService authService,
        IAuthState authState,
        ILogger<ApiClient> logger)
    {
        _httpClientFactory = httpClientFactory;
        _tokenStore = tokenStore;
        _authService = authService;
        _authState = authState;
        _logger = logger;
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

        var attempt = await ExecuteWithTransientRetryAsync(client, method, url, bodyJson, ct);

        if (attempt.IsSuccess)
            return Deserialize<T>(attempt.Body, attempt.StatusCode);

        // Refresh transparente: solo si el backend devuelve 401 con AUTH_REQUIRED.
        // Para INVALID_CREDENTIALS o SESSION_EXPIRED no hay nada que refrescar.
        if (attempt.StatusCode == HttpStatusCode.Unauthorized
            && string.Equals(attempt.ErrorCode, ApiErrorCodes.AuthRequired, StringComparison.Ordinal)
            && await TryRefreshAsync(ct))
        {
            attempt = await ExecuteWithTransientRetryAsync(client, method, url, bodyJson, ct);
            if (attempt.IsSuccess)
                return Deserialize<T>(attempt.Body, attempt.StatusCode);
        }

        throw new ApiException(
            attempt.ErrorCode ?? ApiErrorCodes.InternalError,
            attempt.ErrorMessage ?? $"Error HTTP {(int)attempt.StatusCode}",
            (int)attempt.StatusCode);
    }

    /// <summary>
    /// Ejecuta la request y, si falla por red transitoria (<see cref="HttpRequestException"/>
    /// o <see cref="TaskCanceledException"/> de timeout), espera 500ms y reintenta una vez.
    /// Errores HTTP 4xx/5xx no se reintentan — esos viajan en <see cref="HttpAttempt"/>.
    /// </summary>
    private async Task<HttpAttempt> ExecuteWithTransientRetryAsync(
        HttpClient client,
        HttpMethod method,
        string url,
        string? bodyJson,
        CancellationToken ct)
    {
        var sw = Stopwatch.StartNew();
        try
        {
            var attempt = await ExecuteAsync(client, method, url, bodyJson, ct);
            sw.Stop();
            LogRequest(method, url, attempt, sw.ElapsedMilliseconds, retried: false);
            return attempt;
        }
        catch (HttpRequestException ex) when (!ct.IsCancellationRequested)
        {
            _logger.LogWarning("{Method} {Url} red caída ({Duration}ms) — reintentando: {Error}",
                method, url, sw.ElapsedMilliseconds, ex.Message);
            await Task.Delay(500, ct);
            sw.Restart();
            var attempt = await ExecuteAsync(client, method, url, bodyJson, ct);
            sw.Stop();
            LogRequest(method, url, attempt, sw.ElapsedMilliseconds, retried: true);
            return attempt;
        }
        catch (TaskCanceledException) when (!ct.IsCancellationRequested)
        {
            // Timeout del HttpClient (no cancelación del usuario).
            _logger.LogWarning("{Method} {Url} timeout ({Duration}ms) — reintentando",
                method, url, sw.ElapsedMilliseconds);
            await Task.Delay(500, ct);
            sw.Restart();
            var attempt = await ExecuteAsync(client, method, url, bodyJson, ct);
            sw.Stop();
            LogRequest(method, url, attempt, sw.ElapsedMilliseconds, retried: true);
            return attempt;
        }
    }

    private void LogRequest(HttpMethod method, string url, HttpAttempt attempt, long durationMs, bool retried)
    {
        // Sin cuerpos: solo metadata. Para no leakear PII en los logs.
        var statusCode = (int)attempt.StatusCode;
        var retryTag = retried ? " [reintento]" : string.Empty;
        if (attempt.IsSuccess)
        {
            _logger.LogInformation("{Method} {Url} → {Status} ({Duration}ms){Retry}",
                method, url, statusCode, durationMs, retryTag);
        }
        else
        {
            _logger.LogWarning("{Method} {Url} → {Status} {Code} ({Duration}ms){Retry}",
                method, url, statusCode, attempt.ErrorCode ?? "-", durationMs, retryTag);
        }
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

        // GetValidAccessTokenAsync lee de cache en memoria y dispara refresh
        // proactivo si quedan menos de 60s. Evita el round-trip a SecureStorage
        // por cada request.
        var accessToken = await _authService.GetValidAccessTokenAsync(ct);
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
    /// Refresh reactivo tras 401. La coordinación entre múltiples requests
    /// concurrentes que disparan refresh la maneja <see cref="IAuthService.RefreshAsync"/>
    /// internamente con su propio lock — aquí solo lo invocamos. Si el refresh
    /// falla con error de la API, propagamos <see cref="IAuthState.RaiseSessionExpired"/>;
    /// si falla por red devolvemos <c>false</c> y la request original lanza su error.
    /// </summary>
    private async Task<bool> TryRefreshAsync(CancellationToken ct)
    {
        var refreshToken = await _tokenStore.GetRefreshTokenAsync();
        if (string.IsNullOrEmpty(refreshToken))
        {
            _authState.RaiseSessionExpired();
            return false;
        }

        try
        {
            await _authService.RefreshAsync(refreshToken, ct);
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
