using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class HomeView : ContentView
{
    private static readonly string[] MonthAbbreviations = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
    private IDispatcherTimer? _timer;
    private readonly HomeViewModel _viewModel = new();

    public HomeView()
    {
        InitializeComponent();
        BindingContext = _viewModel;
        UpdateDateTime();
        LoadData();
    }

    protected override void OnHandlerChanged()
    {
        base.OnHandlerChanged();

        if (Handler is not null && _timer is null)
        {
            _timer = Dispatcher.CreateTimer();
            _timer.Interval = TimeSpan.FromSeconds(1);
            _timer.Tick += (_, _) => UpdateDateTime();
            _timer.Start();
        }
        else if (Handler is null && _timer is not null)
        {
            _timer.Stop();
            _timer = null;
        }
    }

    private void UpdateDateTime()
    {
        var now = DateTime.Now;
        DateTimeLabel.Text = $"{now.Day} {MonthAbbreviations[now.Month - 1]} {now.Year}  ·  {now:HH:mm:ss}";
    }

    // ── Load data from MockDataService ───────────────────

    /// <summary>Reload all dashboard data. Can be called externally after invoice changes.</summary>
    public void RefreshData() => LoadData();

    private void LoadData()
    {
        _viewModel.LoadData();
    }

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

    private void OpenInvoiceViewer(ChepaMotos.Models.Invoice invoice)
    {
        var page = new InvoiceViewerPage(invoice);
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
