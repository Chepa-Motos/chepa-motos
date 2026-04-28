using System.Net.Http.Json;
using System.Text.Json;
using ChepaMotos.Models;

namespace ChepaMotos.Services.Auth;

/// <summary>
/// Implementación de <see cref="IAuthService"/>. Usa <see cref="IHttpClientFactory"/>
/// directamente (no <see cref="Api.IApiClient"/>) para evitar el ciclo de DI:
/// <c>ApiClient → AuthService.RefreshAsync</c> ya implica una dependencia, y si
/// además <c>AuthService → ApiClient</c>, el contenedor falla al resolver.
/// </summary>
public sealed class AuthService : IAuthService
{
    private static readonly JsonSerializerOptions JsonOptions = new();

    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ITokenStore _tokenStore;
    private readonly IAuthState _authState;

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

        await _tokenStore.SaveTokensAsync(tokens.AccessToken, tokens.RefreshToken);
        PublishSessionFromAccessToken(tokens.AccessToken, fallbackUsername: username);

        return tokens;
    }

    public async Task<AuthTokens> RefreshAsync(string refreshToken, CancellationToken ct = default)
    {
        var tokens = await PostAsync<AuthTokens>(
            "auth/refresh",
            new RefreshTokenRequest { RefreshToken = refreshToken },
            ct);

        await _tokenStore.SaveTokensAsync(tokens.AccessToken, tokens.RefreshToken);
        PublishSessionFromAccessToken(tokens.AccessToken, fallbackUsername: _authState.Username ?? string.Empty);

        return tokens;
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
        _authState.ClearSession();
    }

    public async Task TryRestoreSessionAsync(CancellationToken ct = default)
    {
        var accessToken = await _tokenStore.GetAccessTokenAsync();
        if (string.IsNullOrEmpty(accessToken))
            return;

        PublishSessionFromAccessToken(accessToken, fallbackUsername: string.Empty);
    }

    private void PublishSessionFromAccessToken(string accessToken, string fallbackUsername)
    {
        var claims = JwtClaimsParser.Parse(accessToken);
        var username = !string.IsNullOrWhiteSpace(claims.Username) ? claims.Username! : fallbackUsername;
        if (string.IsNullOrWhiteSpace(username))
            return;

        _authState.SetSession(username, claims.Roles);
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
