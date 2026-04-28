using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class LoginPage : ContentPage
{
    private readonly LoginViewModel _viewModel;

    public LoginPage(LoginViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        BindingContext = _viewModel;

        _viewModel.LoginSucceeded += (_, _) => LoginSucceeded?.Invoke(this, EventArgs.Empty);
        _viewModel.ContinueWithoutSessionRequested += (_, _) => ContinueRequested?.Invoke(this, EventArgs.Empty);
    }

    /// <summary>Disparado cuando el login termina con éxito.</summary>
    public event EventHandler? LoginSucceeded;

    /// <summary>Disparado cuando el usuario decide saltarse el login.</summary>
    public event EventHandler? ContinueRequested;

    /// <summary>
    /// Marca el banner inline con el mensaje de sesión expirada. Se usa cuando
    /// la shell vuelve a Login tras un refresh fallido. Llamar antes de mostrar
    /// la página.
    /// </summary>
    public void ShowSessionExpiredMessage()
    {
        _viewModel.ErrorMessage = "Tu sesión expiró. Inicia sesión de nuevo.";
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();

        // Si hay un token persistido en SecureStorage, el ViewModel disparará
        // LoginSucceeded y la shell saltará directo a MainLayout.
        await _viewModel.TryRestoreSessionAsync();

        // Auto-focus en el campo usuario al entrar.
        Dispatcher.Dispatch(() => UsernameEntry.Focus());
    }

    private void OnPasswordCompleted(object? sender, EventArgs e)
    {
        if (_viewModel.LoginCommand.CanExecute(null))
            _viewModel.LoginCommand.Execute(null);
    }

    private void OnContinueWithoutSessionTapped(object? sender, TappedEventArgs e)
    {
        if (_viewModel.ContinueWithoutSessionCommand.CanExecute(null))
            _viewModel.ContinueWithoutSessionCommand.Execute(null);
    }
}
