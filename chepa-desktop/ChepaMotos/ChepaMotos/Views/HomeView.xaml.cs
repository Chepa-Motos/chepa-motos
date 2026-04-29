using ChepaMotos.Models;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class HomeView : ContentView
{
    private static readonly string[] MonthAbbreviations = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

    private readonly HomeViewModel _viewModel;
    private readonly Func<Invoice, InvoiceViewerPage> _viewerFactory;
    private IDispatcherTimer? _timer;
    private bool _hasLoadedOnce;

    public HomeView(HomeViewModel viewModel, Func<Invoice, InvoiceViewerPage> viewerFactory)
    {
        InitializeComponent();
        _viewModel = viewModel;
        _viewerFactory = viewerFactory;
        BindingContext = _viewModel;
        UpdateDateTime();
    }

    protected override void OnHandlerChanged()
    {
        base.OnHandlerChanged();

        if (Handler is not null)
        {
            if (_timer is null)
            {
                _timer = Dispatcher.CreateTimer();
                _timer.Interval = TimeSpan.FromSeconds(1);
                _timer.Tick += (_, _) => UpdateDateTime();
                _timer.Start();
            }

            // Primera vez que se monta → carga inicial.
            if (!_hasLoadedOnce)
            {
                _hasLoadedOnce = true;
                _ = _viewModel.ReloadAsync();
            }
        }
        else
        {
            // Desnavegación: paramos el timer y cancelamos requests en vuelo
            // para que no actualicen la UI desmontada.
            if (_timer is not null)
            {
                _timer.Stop();
                _timer = null;
            }
            _viewModel.CancelOngoingOperation();
        }
    }

    private void UpdateDateTime()
    {
        var now = DateTime.Now;
        DateTimeLabel.Text = $"{now.Day} {MonthAbbreviations[now.Month - 1]} {now.Year}  ·  {now:HH:mm:ss}";
    }

    /// <summary>Recarga los datos. Lo invoca <c>MainLayout</c> después de crear/cancelar facturas.</summary>
    public void RefreshData() => _ = _viewModel.ReloadAsync();

    private void OnInvoiceRowTapped(object? sender, TappedEventArgs e)
    {
        if (sender is not Grid grid || grid.BindingContext is not HomeInvoiceRowViewModel row)
            return;

        OpenInvoiceViewer(row.SourceInvoice);
    }

    private static void OnInvoiceRowPointerEntered(object? sender, PointerEventArgs e)
    {
        if (sender is Grid grid)
            grid.BackgroundColor = Color.FromArgb("#F7F6F3");
    }

    private static void OnInvoiceRowPointerExited(object? sender, PointerEventArgs e)
    {
        if (sender is Grid grid)
            grid.BackgroundColor = Colors.Transparent;
    }

    private void OpenInvoiceViewer(Invoice invoice)
    {
        var page = _viewerFactory(invoice);
        page.InvoiceCancelled += () => MainThread.BeginInvokeOnMainThread(RefreshData);
        var isService = invoice.InvoiceType == "SERVICE";
        var window = new Window(page)
        {
            Title = $"Factura #{invoice.Id:D3} — {(isService ? "Servicio" : "Venta")}",
            Width = 720,
            Height = isService ? 600 : 500,
            MinimumWidth = 600,
            MinimumHeight = 400,
        };
        Application.Current?.OpenWindow(window);
    }
}
