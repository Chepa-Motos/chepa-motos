namespace ChepaMotos.Services.Auth;

/// <summary>
/// Persistencia de tokens de autenticación. Implementación por defecto usa
/// <see cref="Microsoft.Maui.Storage.SecureStorage"/> (almacenamiento cifrado nativo).
/// </summary>
public interface ITokenStore
{
    Task<string?> GetAccessTokenAsync();
    Task<string?> GetRefreshTokenAsync();
    Task SaveTokensAsync(string accessToken, string refreshToken);
    Task ClearAsync();
}
