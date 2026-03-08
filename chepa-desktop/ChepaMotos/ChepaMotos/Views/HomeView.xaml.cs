using ChepaMotos.Models;
using ChepaMotos.Services;

namespace ChepaMotos.Views;

public partial class HomeView : ContentView
{
    private IDispatcherTimer? _timer;

    public HomeView()
    {
        InitializeComponent();
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
    }

    private void UpdateDateTime()
    {
        var now = DateTime.Now;
        var months = new[] { "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic" };
        DateTimeLabel.Text = $"{now.Day} {months[now.Month - 1]} {now.Year}  ·  {now:HH:mm:ss}";
    }

    // ── Load data from MockDataService ───────────────────

    /// <summary>Reload all dashboard data. Can be called externally after invoice changes.</summary>
    public void RefreshData() => LoadData();

    private void LoadData()
    {
        LoadKpis();
        // TODO: [API] Replace with: var invoices = await InvoiceService.GetInvoices(date: DateTime.Today)
        // Maps to: GET /invoices?date=YYYY-MM-DD
        BuildInvoiceRows(MockDataService.GetInvoices(date: DateTime.Today));
        BuildMechanicsList();
    }

    private void LoadKpis()
    {
        // TODO: [API] Replace with aggregated calls or a dedicated dashboard endpoint
        // KPI data is computed client-side from GET /invoices + GET /mechanics
        var (total, shopCut, avg, count, active, registered) = MockDataService.GetTodayKpis();

        KpiTotalValue.Text = MockDataService.FormatCurrency(total);
        KpiTotalSubtitle.Text = $"{count} facturas hoy";

        KpiShopValue.Text = MockDataService.FormatCurrency(shopCut);

        KpiAvgValue.Text = MockDataService.FormatCurrency(avg);

        KpiMechanicsValue.Text = $"{active}";
        KpiMechanicsSubtitle.Text = $"de {registered} registrados";
    }

    // ── Build clickable invoice rows ─────────────────────

    private void BuildInvoiceRows(List<Invoice> invoices)
    {
        InvoiceRowsContainer.Children.Clear();

        foreach (var invoice in invoices)
        {
            var isService = invoice.InvoiceType == "SERVICE";

            var grid = new Grid
            {
                ColumnDefinitions =
                [
                    new ColumnDefinition(new GridLength(60)),
                    new ColumnDefinition(new GridLength(80)),
                    new ColumnDefinition(GridLength.Star),
                    new ColumnDefinition(new GridLength(100)),
                    new ColumnDefinition(new GridLength(100)),
                    new ColumnDefinition(new GridLength(80)),
                ],
                Padding = new Thickness(16, 10),
                Opacity = invoice.IsCancelled ? 0.5 : 1.0,
            };

            // # column
            var idLabel = new Label
            {
                Text = $"{invoice.Id:D3}",
                Style = (Style)Application.Current!.Resources["TableCellMono"],
            };
            Grid.SetColumn(idLabel, 0);
            grid.Children.Add(idLabel);

            // Type badge
            var typeBadge = new Border
            {
                Style = (Style)Application.Current.Resources["Badge"],
                BackgroundColor = isService
                    ? (Color)Application.Current.Resources["AccentLight"]
                    : (Color)Application.Current.Resources["BlueLight"],
                Content = new Label
                {
                    Text = isService ? "Servicio" : "Venta",
                    FontFamily = "IBMPlexSansMedium",
                    FontSize = 11,
                    TextColor = isService
                        ? (Color)Application.Current.Resources["Accent"]
                        : (Color)Application.Current.Resources["Blue"],
                },
            };
            Grid.SetColumn(typeBadge, 1);
            grid.Children.Add(typeBadge);

            // Plate / Buyer column
            if (isService && invoice.Vehicle is not null)
            {
                var plateBadge = new Border
                {
                    Style = (Style)Application.Current.Resources["Badge"],
                    BackgroundColor = (Color)Application.Current.Resources["Surface2"],
                    Stroke = (Color)Application.Current.Resources["Border"],
                    StrokeThickness = 1,
                    Content = new Label
                    {
                        Text = invoice.Vehicle.Plate,
                        FontFamily = "IBMPlexMono",
                        FontSize = 11,
                        TextColor = (Color)Application.Current.Resources["TextPrimary"],
                    },
                };
                Grid.SetColumn(plateBadge, 2);
                grid.Children.Add(plateBadge);
            }
            else
            {
                var buyerLabel = new Label
                {
                    Text = invoice.BuyerName ?? "—",
                    Style = (Style)Application.Current.Resources["TableCell"],
                };
                Grid.SetColumn(buyerLabel, 2);
                grid.Children.Add(buyerLabel);
            }

            // Mechanic
            var mechanicLabel = new Label
            {
                Text = invoice.Mechanic?.Name?.Split(' ')[0] ?? "—",
                Style = (Style)Application.Current.Resources["TableCell"],
                TextColor = invoice.Mechanic is null
                    ? (Color)Application.Current.Resources["TextMuted"]
                    : (Color)Application.Current.Resources["TextPrimary"],
            };
            Grid.SetColumn(mechanicLabel, 3);
            grid.Children.Add(mechanicLabel);

            // Total
            var totalLabel = new Label
            {
                Text = MockDataService.FormatCurrency(invoice.TotalAmount),
                Style = (Style)Application.Current.Resources["TableCellMono"],
                HorizontalTextAlignment = TextAlignment.End,
            };
            Grid.SetColumn(totalLabel, 4);
            grid.Children.Add(totalLabel);

            // Time
            var timeLabel = new Label
            {
                Text = invoice.CreatedAt.ToString("HH:mm"),
                Style = (Style)Application.Current.Resources["TableCellMono"],
                HorizontalTextAlignment = TextAlignment.End,
                TextColor = (Color)Application.Current.Resources["TextMuted"],
            };
            Grid.SetColumn(timeLabel, 5);
            grid.Children.Add(timeLabel);

            // Tap to open viewer
            var capturedInvoice = invoice;
            var tapGesture = new TapGestureRecognizer();
            tapGesture.Tapped += (_, _) => OpenInvoiceViewer(capturedInvoice);
            grid.GestureRecognizers.Add(tapGesture);

            // Hover effect
            var pointerGesture = new PointerGestureRecognizer();
            pointerGesture.PointerEntered += (_, _) => grid.BackgroundColor = Color.FromArgb("#F7F6F3");
            pointerGesture.PointerExited += (_, _) => grid.BackgroundColor = Colors.Transparent;
            grid.GestureRecognizers.Add(pointerGesture);

            InvoiceRowsContainer.Children.Add(grid);
            InvoiceRowsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current.Resources["Border"],
            });
        }
    }

    // ── Build mechanics sidebar list ─────────────────────

    private void BuildMechanicsList()
    {
        MechanicsContainer.Children.Clear();

        // TODO: [API] Replace with: var activeMechanics = await MechanicService.GetMechanics(active: true)
        // Maps to: GET /mechanics?active=true
        var activeMechanics = MockDataService.GetMechanics(activeOnly: true);
        // TODO: [API] This is computed client-side from GET /invoices?date=today&type=SERVICE
        var invoiceCounts = MockDataService.GetTodayInvoiceCountByMechanic();

        foreach (var mechanic in activeMechanics)
        {
            var data = invoiceCounts.GetValueOrDefault(mechanic.Id, (0, 0m));
            var count = data.Item1;

            var grid = new Grid
            {
                ColumnDefinitions =
                [
                    new ColumnDefinition(GridLength.Star),
                    new ColumnDefinition(GridLength.Auto),
                    new ColumnDefinition(GridLength.Auto),
                ],
                Padding = new Thickness(16, 10),
            };

            var nameLabel = new Label
            {
                Text = mechanic.Name.Split(' ')[0],
                Style = (Style)Application.Current!.Resources["TableCell"],
            };
            Grid.SetColumn(nameLabel, 0);

            var badge = new Border
            {
                Style = (Style)Application.Current.Resources["Badge"],
                BackgroundColor = (Color)Application.Current.Resources["GreenLight"],
                Content = new Label
                {
                    Text = "Activo",
                    FontFamily = "IBMPlexSansMedium",
                    FontSize = 11,
                    TextColor = (Color)Application.Current.Resources["Green"],
                },
            };
            Grid.SetColumn(badge, 1);

            var countLabel = new Label
            {
                Text = $"{count} fact.",
                FontFamily = "IBMPlexMono",
                FontSize = 11,
                TextColor = (Color)Application.Current.Resources["TextMuted"],
                VerticalTextAlignment = TextAlignment.Center,
                Margin = new Thickness(8, 0, 0, 0),
            };
            Grid.SetColumn(countLabel, 2);

            grid.Children.Add(nameLabel);
            grid.Children.Add(badge);
            grid.Children.Add(countLabel);

            MechanicsContainer.Children.Add(grid);
            MechanicsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current.Resources["Border"],
            });
        }

        // Remove last divider
        if (MechanicsContainer.Children.Count > 0)
            MechanicsContainer.Children.RemoveAt(MechanicsContainer.Children.Count - 1);
    }

    private void OpenInvoiceViewer(Invoice invoice)
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
