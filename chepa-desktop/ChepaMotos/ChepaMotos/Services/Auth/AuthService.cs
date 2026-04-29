using System.Net.Http.Json;
using System.Text.Json;
using ChepaMotos.Models;

namespace ChepaMotos.Services.Auth;

/// <summary>
/// Implementación de <see cref="IAuthService"/>. Usa <see cref="IHttpClientFactory"/>
/// directamente (no <see cref="Api.IApiClient"/>) para evitar el ciclo de DI:
/// <c>ApiClient → AuthService.RefreshAsync</c> ya implica una dependencia, y si
/// además <c>AuthService → ApiClient</c>, el contenedor falla al resolver.
///
/// Mantiene el access token en memoria con su <c>exp</c> parseado del JWT,
/// para que cada request del <see cref="Api.ApiClient"/> no tenga que ir a
/// <c>SecureStorage</c> y para refrescar proactivamente cuando le quedan
/// menos de 60 segundos.
/// </summary>
public sealed class AuthService : IAuthService
{
    private static readonly JsonSerializerOptions JsonOptions = new();

    /// <summary>
    /// Margen de seguridad: si el token expira en menos de este intervalo,
    /// disparamos refresh proactivo antes de devolverlo. 60s es suficiente
    /// para que la request en curso termine antes de la expiración real.
    /// </summary>
    private static readonly TimeSpan ProactiveRefreshThreshold = TimeSpan.FromSeconds(60);

    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ITokenStore _tokenStore;
    private readonly IAuthState _authState;

    /// <summary>Lock para evitar dos refreshes concurrentes (proactivo + reactivo).</summary>
    private readonly SemaphoreSlim _refreshLock = new(1, 1);

    /// <summary>Cache en memoria del access token actual.</summary>
    private string? _cachedAccessToken;
    private DateTimeOffset? _accessExpiresAt;

    /// <summary>True después del primer intento de leer SecureStorage (con o sin éxito).</summary>
    private bool _restoredFromStorage;

    public AuthService(IHttpClientFactory httpClientFactory, ITokenStore tokenStore, IAuthState authState)
    {
        _httpClientFactory = httpClientFactory;
        _tokenStore = tokenStore;
        _authState = authState;
    }

    public async Task<AuthTokens> LoginAsync(string username, string password, CancellationToken ct = default)
    {
        var tokens = await PostAsync<AuthTokens>(
            "auth/login",
            new LoginRequest { Username = username, Password = password },
            ct);

        await PersistAndCacheAsync(tokens, fallbackUsername: username);
        return tokens;
    }

    public async Task<AuthTokens> RefreshAsync(string refreshToken, CancellationToken ct = default)
    {
        await _refreshLock.WaitAsync(ct);
        try
        {
            // Otro caller pudo haber refrescado mientras esperábamos el lock.
            // Si el refresh token actual ya no es el que recibimos, devolvemos
            // el access token actual sin volver a llamar al backend (el viejo
            // refresh token ya está rotado y rechazado).
            var current = await _tokenStore.GetRefreshTokenAsync();
            if (!string.IsNullOrEmpty(current) && current != refreshToken && _cachedAccessToken is not null)
            {
                return new AuthTokens
                {
                    AccessToken = _cachedAccessToken,
                    RefreshToken = current,
                    TokenType = "Bearer",
                    ExpiresIn = (long)((_accessExpiresAt ?? DateTimeOffset.UtcNow) - DateTimeOffset.UtcNow).TotalSeconds,
                };
            }

            var tokens = await PostAsync<AuthTokens>(
                "auth/refresh",
                new RefreshTokenRequest { RefreshToken = refreshToken },
                ct);

            await PersistAndCacheAsync(tokens, fallbackUsername: _authState.Username ?? string.Empty);
            return tokens;
        }
        finally
        {
            _refreshLock.Release();
        }
    }

    public async Task LogoutAsync(CancellationToken ct = default)
    {
        var refreshToken = await _tokenStore.GetRefreshTokenAsync();
        if (!string.IsNullOrEmpty(refreshToken))
        {
            try
            {
                await PostAsync<JsonElement>(
                    "auth/logout",
                    new RefreshTokenRequest { RefreshToken = refreshToken },
                    ct);
            }
            catch (ApiException) { /* best-effort: limpiamos local aunque el servidor falle */ }
            catch (HttpRequestException) { /* sin red: limpiamos local */ }
            catch (TaskCanceledException) { /* timeout: limpiamos local */ }
        }

        await _tokenStore.ClearAsync();
        ClearCache();
        _authState.RaiseLoggedOut();
    }

    public async Task TryRestoreSessionAsync(CancellationToken ct = default)
    {
        var accessToken = await _tokenStore.GetAccessTokenAsync();
        _restoredFromStorage = true;
        if (string.IsNullOrEmpty(accessToken))
            return;

        var claims = JwtClaimsParser.Parse(accessToken);
        _cachedAccessToken = accessToken;
        _accessExpiresAt = claims.ExpiresAt;
        PublishSession(claims, fallbackUsername: string.Empty);
    }

    public async Task<string?> GetValidAccessTokenAsync(CancellationToken ct = default)
    {
        // Lazy load del SecureStorage la primera vez (la app pudo arrancar sin
        // pasar por TryRestoreSessionAsync — ej. continuar sin sesión y luego
        // hacer login).
        if (!_restoredFromStorage)
            await TryRestoreSessionAsync(ct);

        if (string.IsNullOrEmpty(_cachedAccessToken))
            return null;

        // Si no tenemos info de expiración o aún tenemos margen, devolvemos directo.
        if (_accessExpiresAt is null
            || _accessExpiresAt.Value - DateTimeOffset.UtcNow > ProactiveRefreshThreshold)
        {
            return _cachedAccessToken;
        }

        // Refresh proactivo. Si falla, devolvemos el token caché tal cual —
        // el ApiClient lo intentará igualmente y caerá al refresh reactivo
        // del 401 si hace falta.
        var refreshToken = await _tokenStore.GetRefreshTokenAsync();
        if (string.IsNullOrEmpty(refreshToken))
            return _cachedAccessToken;

        try
        {
            var tokens = await RefreshAsync(refreshToken, ct);
            return tokens.AccessToken;
        }
        catch (ApiException)
        {
            return _cachedAccessToken;
        }
        catch (HttpRequestException)
        {
            return _cachedAccessToken;
        }
        catch (TaskCanceledException)
        {
            return _cachedAccessToken;
        }
    }

    private async Task PersistAndCacheAsync(AuthTokens tokens, string fallbackUsername)
    {
        await _tokenStore.SaveTokensAsync(tokens.AccessToken, tokens.RefreshToken);
        var claims = JwtClaimsParser.Parse(tokens.AccessToken);
        _cachedAccessToken = tokens.AccessToken;
        _accessExpiresAt = claims.ExpiresAt;
        _restoredFromStorage = true;
        PublishSession(claims, fallbackUsername);
    }

    private void PublishSession(JwtClaimsParser.JwtClaims claims, string fallbackUsername)
    {
        var username = !string.IsNullOrWhiteSpace(claims.Username) ? claims.Username! : fallbackUsername;
        if (string.IsNullOrWhiteSpace(username))
            return;
        _authState.SetSession(username, claims.Roles);
    }

    private void ClearCache()
    {
        _cachedAccessToken = null;
        _accessExpiresAt = null;
        // _restoredFromStorage se queda en true: SecureStorage está vacío,
        // no necesitamos releer.
    }

    private async Task<T> PostAsync<T>(string path, object body, CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient(MauiProgram.ApiHttpClientName);
        using var content = JsonContent.Create(body, options: JsonOptions);
        using var response = await client.PostAsync(path, content, ct);

        var responseBody = await response.Content.ReadAsStringAsync(ct);

        if (!response.IsSuccessStatusCode)
            throw BuildException(response.StatusCode, responseBody);

        if (string.IsNullOrWhiteSpace(responseBody))
            throw new ApiException(ApiErrorCodes.InternalError, "Respuesta vacía del servidor", (int)response.StatusCode);

        var envelope = JsonSerializer.Deserialize<ApiResponse<T>>(responseBody, JsonOptions);
        if (envelope is null)
            throw new ApiException(ApiErrorCodes.InternalError, "Respuesta inválida del servidor", (int)response.StatusCode);

        return envelope.Data!;
    }

    private static ApiException BuildException(System.Net.HttpStatusCode statusCode, string body)
    {
        var status = (int)statusCode;
        if (!string.IsNullOrWhiteSpace(body))
        {
            try
            {
                var error = JsonSerializer.Deserialize<ApiErrorResponse>(body, JsonOptions);
                if (error is not null && !string.IsNullOrEmpty(error.Code))
                    return new ApiException(error.Code, error.Message ?? $"Error HTTP {status}", status);
            }
            catch (JsonException) { /* respuesta no-JSON: caemos al fallback */ }
        }
        return new ApiException(ApiErrorCodes.InternalError, $"Error HTTP {status}", status);
    }
}
