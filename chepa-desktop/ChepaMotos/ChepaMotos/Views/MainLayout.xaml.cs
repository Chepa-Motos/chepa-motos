namespace ChepaMotos.Views;

using ChepaMotos.Helpers;
using ChepaMotos.Services;
using ChepaMotos.Services.Auth;
using Microsoft.Extensions.DependencyInjection;

public partial class MainLayout : ContentPage
{
    private readonly Dictionary<string, Border> _navItems;
    private readonly IServiceProvider _services;
    private readonly IAuthState _authState;
    private readonly IAuthService _authService;
    private string _currentNav = "Inicio";

    public MainLayout(IServiceProvider services, IAuthState authState, IAuthService authService)
    {
        _services = services;
        _authState = authState;
        _authService = authService;
        InitializeComponent();

        _navItems = new Dictionary<string, Border>
        {
            ["Inicio"] = NavInicio,
            ["Facturas"] = NavFacturas,
            ["Liquidaciones"] = NavLiquidaciones,
            ["Dashboards"] = NavDashboards,
            ["Mecanicos"] = NavMecanicos,
        };

        // Carga inicial de la vista Inicio.
        ContentArea.Content = _services.GetRequiredService<HomeView>();

        UpdateSessionFooter();
    }

    private void UpdateSessionFooter()
    {
        if (_authState.IsAuthenticated)
        {
            SessionUserLabel.Text = _authState.Username ?? "—";
            SessionRoleLabel.Text = _authState.IsManager ? "GERENTE" : string.Join(", ", _authState.Roles);
            SessionActionButton.Text = "Salir";
        }
        else
        {
            SessionUserLabel.Text = "Modo invitado";
            SessionRoleLabel.Text = "Sin sesión";
            SessionActionButton.Text = "Iniciar sesión";
        }
    }

    private async void OnSessionActionClicked(object? sender, EventArgs e)
    {
        if (!_authState.IsAuthenticated)
        {
            // Modo invitado → llevar al LoginPage. Reusamos el flujo de logout
            // para que App.xaml.cs haga swap a Login.
            _authState.RaiseLoggedOut();
            return;
        }

        bool confirm = await DisplayAlertAsync(
            "Cerrar sesión",
            $"¿Cerrar la sesión de {_authState.Username}?",
            "Sí, cerrar",
            "Cancelar");

        if (!confirm) return;

        SessionActionButton.IsEnabled = false;
        try
        {
            await _authService.LogoutAsync();
            // App.xaml.cs escucha LoggedOut y hace el swap a LoginPage.
        }
        finally
        {
            SessionActionButton.IsEnabled = true;
        }
    }

    private void OnNavTapped(object? sender, TappedEventArgs e)
    {
        if (e.Parameter is not string target || target == _currentNav)
            return;

        // Reset all nav items to unselected
        foreach (var (_, border) in _navItems)
        {
            border.BackgroundColor = Colors.Transparent;
            if (border.Content is Label label)
                label.Style = Application.Current!.Resources["SidebarNavLabel"] as Style;
        }

        // Highlight selected
        if (_navItems.TryGetValue(target, out var selectedBorder))
        {
            selectedBorder.BackgroundColor = Color.FromArgb("#2A2822");
            if (selectedBorder.Content is Label selectedLabel)
                selectedLabel.Style = Application.Current!.Resources["SidebarNavLabelSelected"] as Style;
        }

        // Swap content. Inicio/Facturas/Liquidaciones/Mecanicos ya migradas a DI.
        ContentArea.Content = target switch
        {
            "Inicio" => _services.GetRequiredService<HomeView>(),
            "Facturas" => _services.GetRequiredService<InvoicesView>(),
            "Liquidaciones" => _services.GetRequiredService<LiquidationsView>(),
            "Dashboards" => _services.GetRequiredService<DashboardsView>(),
            "Mecanicos" => _services.GetRequiredService<MechanicsView>(),
            _ => _services.GetRequiredService<HomeView>(),
        };

        _currentNav = target;
    }

    private void OnServiceInvoiceClicked(object? sender, EventArgs e)
    {
        var page = _services.GetRequiredService<ServiceInvoicePage>();
        page.InvoiceConfirmed += () => MainThread.BeginInvokeOnMainThread(() =>
        {
            ToastService.ShowSuccess(this, "Factura de servicio registrada");
            RefreshCurrentView();
        });
        var window = new Window(page)
        {
            Title = "Factura de Servicio",
            Width = 720,
            Height = 680,
            MinimumWidth = 620,
            MinimumHeight = 500,
        };
        AttachCloseConfirmation(window, page);
        Application.Current?.OpenWindow(window);
    }

    private void OnDeliveryInvoiceClicked(object? sender, EventArgs e)
    {
        var page = _services.GetRequiredService<DeliveryInvoicePage>();
        page.InvoiceConfirmed += () => MainThread.BeginInvokeOnMainThread(() =>
        {
            ToastService.ShowSuccess(this, "Factura de venta registrada");
            RefreshCurrentView();
        });
        var window = new Window(page)
        {
            Title = "Factura de Venta",
            Width = 720,
            Height = 600,
            MinimumWidth = 620,
            MinimumHeight = 450,
        };
        AttachCloseConfirmation(window, page);
        Application.Current?.OpenWindow(window);
    }

    /// <summary>
    /// Recarga datos de la View actual sin perder estado local (filtros,
    /// fecha seleccionada, scroll). Si la View implementa <see cref="IRefreshable"/>,
    /// solo se le pide refrescar; no se reconstruye.
    /// </summary>
    private void RefreshCurrentView()
    {
        if (ContentArea.Content is IRefreshable refreshable)
        {
            _ = refreshable.RefreshAsync();
            return;
        }

        // Fallback: si la View actual aún no migró a IRefreshable (solo
        // DashboardsView en este punto), reconstruimos.
        ContentArea.Content = _currentNav switch
        {
            "Facturas" => _services.GetRequiredService<InvoicesView>(),
            "Liquidaciones" => _services.GetRequiredService<LiquidationsView>(),
            "Dashboards" => _services.GetRequiredService<DashboardsView>(),
            "Mecanicos" => _services.GetRequiredService<MechanicsView>(),
            "Inicio" => _services.GetRequiredService<HomeView>(),
            _ => ContentArea.Content,
        };
    }

    /// <summary>
    /// Intercepts the native window X button to show a confirmation dialog.
    /// </summary>
    private static void AttachCloseConfirmation(Window window, ContentPage page)
    {
#if WINDOWS
        var closeHooked = false;
        var confirmationOpen = false;

        bool TryHookNativeClose()
        {
            if (closeHooked)
                return true;

            if (window.Handler?.PlatformView is Microsoft.UI.Xaml.Window nativeWindow)
            {
                var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(nativeWindow);
                var windowId = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(hwnd);
                var appWindow = Microsoft.UI.Windowing.AppWindow.GetFromWindowId(windowId);
                closeHooked = true;

                appWindow.Closing += async (appWin, args) =>
                {
                    if (confirmationOpen)
                    {
                        args.Cancel = true;
                        return;
                    }

                    confirmationOpen = true;
                    args.Cancel = true;

                    bool confirm = await page.DisplayAlertAsync(
                        "Cerrar factura",
                        "¿Estás seguro de que deseas cerrar? Se perderán los datos ingresados.",
                        "Sí, cerrar",
                        "Volver");

                    if (confirm)
                        Application.Current?.CloseWindow(window);

                    confirmationOpen = false;
                };

                return true;
            }

            return false;
        }

        EventHandler? handlerChanged = null;
        handlerChanged = (_, _) =>
        {
            if (TryHookNativeClose() && handlerChanged is not null)
                window.HandlerChanged -= handlerChanged;
        };

        if (!TryHookNativeClose())
            window.HandlerChanged += handlerChanged;
#endif
    }
}
