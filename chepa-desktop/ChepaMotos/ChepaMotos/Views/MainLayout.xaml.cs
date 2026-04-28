namespace ChepaMotos.Views;

using ChepaMotos.Services;
using Microsoft.Extensions.DependencyInjection;

public partial class MainLayout : ContentPage
{
    private readonly Dictionary<string, Border> _navItems;
    private readonly IServiceProvider _services;
    private string _currentNav = "Inicio";

    public MainLayout(IServiceProvider services)
    {
        _services = services;
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
            "Dashboards" => new DashboardsView(),
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

    /// <summary>Refresh the currently visible view after data changes.</summary>
    private void RefreshCurrentView()
    {
        if (ContentArea.Content is HomeView homeView)
            homeView.RefreshData();
        else
        {
            // Rebuild the current view to pick up changes
            ContentArea.Content = _currentNav switch
            {
                "Facturas" => _services.GetRequiredService<InvoicesView>(),
                "Liquidaciones" => _services.GetRequiredService<LiquidationsView>(),
                "Mecanicos" => _services.GetRequiredService<MechanicsView>(),
                "Inicio" => _services.GetRequiredService<HomeView>(),
                _ => ContentArea.Content,
            };
        }
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
