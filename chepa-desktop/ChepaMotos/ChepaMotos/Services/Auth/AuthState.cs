namespace ChepaMotos.Services.Auth;

public sealed class AuthState : IAuthState
{
    private const string ManagerRole = "GERENTE";

    public bool IsAuthenticated => Username is not null;
    public string? Username { get; private set; }
    public IReadOnlyList<string> Roles { get; private set; } = Array.Empty<string>();
    public bool IsManager => Roles.Contains(ManagerRole, StringComparer.OrdinalIgnoreCase);

    public event EventHandler? SessionExpired;
    public event EventHandler? AuthChanged;
    public event EventHandler? LoggedOut;

    public void SetSession(string username, IEnumerable<string> roles)
    {
        Username = username;
        Roles = roles?.Where(r => !string.IsNullOrWhiteSpace(r)).ToArray() ?? Array.Empty<string>();
        AuthChanged?.Invoke(this, EventArgs.Empty);
    }

    public void ClearSession()
    {
        Username = null;
        Roles = Array.Empty<string>();
        AuthChanged?.Invoke(this, EventArgs.Empty);
    }

    public void RaiseSessionExpired()
    {
        ClearSession();
        SessionExpired?.Invoke(this, EventArgs.Empty);
    }

    public void RaiseLoggedOut()
    {
        ClearSession();
        LoggedOut?.Invoke(this, EventArgs.Empty);
    }
}
