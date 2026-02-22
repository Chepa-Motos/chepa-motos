namespace ChepaMotos.Views;

public partial class MainLayout : ContentPage
{
    private readonly Dictionary<string, Border> _navItems;
    private string _currentNav = "Inicio";

    public MainLayout()
    {
        InitializeComponent();

        _navItems = new Dictionary<string, Border>
        {
            ["Inicio"] = NavInicio,
            ["Facturas"] = NavFacturas,
            ["Liquidaciones"] = NavLiquidaciones,
            ["Dashboards"] = NavDashboards,
            ["Mecanicos"] = NavMecanicos,
        };
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
                label.Style = (Style)Resources.MergedDictionaries
                    .SelectMany(d => d)
                    .FirstOrDefault(kv => kv.Key.ToString() == "SidebarNavLabel").Value
                    ?? Application.Current!.Resources["SidebarNavLabel"] as Style;
        }

        // Highlight selected
        if (_navItems.TryGetValue(target, out var selectedBorder))
        {
            selectedBorder.BackgroundColor = Color.FromArgb("#2A2822");
            if (selectedBorder.Content is Label selectedLabel)
                selectedLabel.Style = Application.Current!.Resources["SidebarNavLabelSelected"] as Style;
        }

        // Swap content
        ContentArea.Content = target switch
        {
            "Inicio" => new HomeView(),
            "Facturas" => new InvoicesView(),
            "Liquidaciones" => new LiquidationsView(),
            "Dashboards" => new DashboardsView(),
            "Mecanicos" => new MechanicsView(),
            _ => new HomeView(),
        };

        _currentNav = target;
    }

    private void OnServiceInvoiceClicked(object? sender, EventArgs e)
    {
        var page = new ServiceInvoicePage();
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
        var page = new DeliveryInvoicePage();
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
    /// Intercepts the native window X button to show a confirmation dialog.
    /// </summary>
    private static void AttachCloseConfirmation(Window window, ContentPage page)
    {
#if WINDOWS
        window.HandlerChanged += (s, e) =>
        {
            if (window.Handler?.PlatformView is Microsoft.UI.Xaml.Window nativeWindow)
            {
                var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(nativeWindow);
                var windowId = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(hwnd);
                var appWindow = Microsoft.UI.Windowing.AppWindow.GetFromWindowId(windowId);

                appWindow.Closing += async (appWin, args) =>
                {
                    args.Cancel = true;

                    bool confirm = await page.DisplayAlertAsync(
                        "Cerrar factura",
                        "¿Estás seguro de que deseas cerrar? Se perderán los datos ingresados.",
                        "Sí, cerrar",
                        "Volver");

                    if (confirm)
                        Application.Current?.CloseWindow(window);
                };
            }
        };
#endif
    }
}
