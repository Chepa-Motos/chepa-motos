using ChepaMotos.Models;
using ChepaMotos.Services.Auth;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ChepaMotos.ViewModels;

public partial class LoginViewModel : ObservableObject
{
    private readonly IAuthService _authService;
    private readonly IAuthState _authState;
    private bool _hasAttemptedRestore;

    [ObservableProperty]
    private string _username = string.Empty;

    [ObservableProperty]
    private string _password = string.Empty;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(LoginButtonText))]
    [NotifyPropertyChangedFor(nameof(IsNotBusy))]
    private bool _isBusy;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(HasError))]
    private string? _errorMessage;

    public bool IsNotBusy => !IsBusy;
    public bool HasError => !string.IsNullOrEmpty(ErrorMessage);
    public string LoginButtonText => IsBusy ? "Iniciando…" : "Iniciar sesión";

    /// <summary>Disparado cuando el login se completa con éxito.</summary>
    public event EventHandler? LoginSucceeded;

    /// <summary>
    /// Disparado cuando el usuario decide saltarse el login. La mayoría de
    /// endpoints son <c>permitAll()</c>, así que el modo lectura/factura
    /// funciona; solo crear mecánico y liquidar requieren rol GERENTE.
    /// </summary>
    public event EventHandler? ContinueWithoutSessionRequested;

    public LoginViewModel(IAuthService authService, IAuthState authState)
    {
        _authService = authService;
        _authState = authState;
    }

    /// <summary>
    /// Intenta restaurar la sesión desde el token persistido en SecureStorage.
    /// Si quedó autenticado, dispara <see cref="LoginSucceeded"/> para que la
    /// shell salte directo a <c>MainLayout</c>.
    /// </summary>
    public async Task TryRestoreSessionAsync()
    {
        if (_hasAttemptedRestore) return;
        _hasAttemptedRestore = true;

        try
        {
            await _authService.TryRestoreSessionAsync();
        }
        catch
        {
            // Si la lectura del SecureStorage falla, simplemente nos quedamos en login.
            return;
        }

        if (_authState.IsAuthenticated)
            LoginSucceeded?.Invoke(this, EventArgs.Empty);
    }

    [RelayCommand]
    private async Task LoginAsync()
    {
        if (IsBusy) return;

        var user = Username.Trim();
        if (string.IsNullOrEmpty(user))
        {
            ErrorMessage = "Ingresa tu usuario";
            return;
        }
        if (string.IsNullOrEmpty(Password))
        {
            ErrorMessage = "Ingresa tu contraseña";
            return;
        }

        ErrorMessage = null;
        IsBusy = true;
        try
        {
            await _authService.LoginAsync(user, Password);
            Password = string.Empty;
            LoginSucceeded?.Invoke(this, EventArgs.Empty);
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.InvalidCredentials)
        {
            ErrorMessage = "Usuario o contraseña incorrectos";
        }
        catch (ApiException ex)
        {
            ErrorMessage = ex.Message;
        }
        catch (HttpRequestException)
        {
            ErrorMessage = "No se pudo conectar al servidor. ¿Está encendido?";
        }
        catch (TaskCanceledException)
        {
            ErrorMessage = "El servidor tardó demasiado en responder";
        }
        finally
        {
            IsBusy = false;
        }
    }

    [RelayCommand]
    private void ContinueWithoutSession()
    {
        if (IsBusy) return;
        ContinueWithoutSessionRequested?.Invoke(this, EventArgs.Empty);
    }
}
