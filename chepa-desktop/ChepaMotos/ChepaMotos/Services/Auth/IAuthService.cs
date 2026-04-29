using ChepaMotos.Models;

namespace ChepaMotos.Services.Auth;

public interface IAuthService
{
    /// <summary>
    /// POST /auth/login. Persiste tokens, decodifica el JWT y publica la sesión
    /// en <see cref="IAuthState"/>. Lanza <see cref="ApiException"/> con
    /// <c>code = INVALID_CREDENTIALS</c> si las credenciales son inválidas.
    /// </summary>
    Task<AuthTokens> LoginAsync(string username, string password, CancellationToken ct = default);

    /// <summary>
    /// POST /auth/refresh con el refresh token actual. El backend rota el
    /// refresh token en cada uso, por eso siempre persistimos el nuevo par.
    /// Lanza <see cref="ApiException"/> con <c>code = SESSION_EXPIRED</c> o
    /// <c>AUTH_REQUIRED</c> si el refresh ya no es válido.
    /// </summary>
    Task<AuthTokens> RefreshAsync(string refreshToken, CancellationToken ct = default);

    /// <summary>
    /// POST /auth/logout. Best-effort: si la red falla limpia igualmente el
    /// estado local.
    /// </summary>
    Task LogoutAsync(CancellationToken ct = default);

    /// <summary>
    /// Restaura la sesión desde el access token persistido (decodifica claims y
    /// publica en <see cref="IAuthState"/>). Llamar al arrancar la app.
    /// </summary>
    Task TryRestoreSessionAsync(CancellationToken ct = default);

    /// <summary>
    /// Devuelve un access token vigente. Si la cache en memoria está vacía,
    /// la rellena desde <c>SecureStorage</c>. Si al token le quedan menos de
    /// 60 segundos, dispara un refresh transparente antes de devolver. Si no
    /// hay sesión, retorna <c>null</c> (no lanza excepción).
    /// </summary>
    Task<string?> GetValidAccessTokenAsync(CancellationToken ct = default);
}
