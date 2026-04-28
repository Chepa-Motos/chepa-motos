namespace ChepaMotos.Services.Auth;

/// <summary>
/// Estado de sesión observable, compartido entre <see cref="IAuthService"/>,
/// <see cref="Api.IApiClient"/> y la UI.
/// </summary>
public interface IAuthState
{
    bool IsAuthenticated { get; }
    string? Username { get; }
    IReadOnlyList<string> Roles { get; }

    /// <summary>True si el usuario tiene rol GERENTE (necesario para operaciones admin).</summary>
    bool IsManager { get; }

    /// <summary>Disparado cuando un refresh falla y el usuario debe volver a iniciar sesión.</summary>
    event EventHandler? SessionExpired;

    /// <summary>Disparado cuando cambia la sesión (login, logout, expiración).</summary>
    event EventHandler? AuthChanged;

    void SetSession(string username, IEnumerable<string> roles);
    void ClearSession();
    void RaiseSessionExpired();
}
