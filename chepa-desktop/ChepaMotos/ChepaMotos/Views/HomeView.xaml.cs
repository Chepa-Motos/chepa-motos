using ChepaMotos.Models;

namespace ChepaMotos.Views;

public partial class HomeView : ContentView
{
    private IDispatcherTimer? _timer;
    private readonly List<Invoice> _sampleInvoices;

    public HomeView()
    {
        InitializeComponent();
        UpdateDateTime();

        _sampleInvoices = BuildSampleInvoices();
        BuildInvoiceRows();
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

    // ── Sample data ──────────────────────────────────────

    private static List<Invoice> BuildSampleInvoices()
    {
        var today = DateTime.Today;

        return
        [
            new Invoice
            {
                Id = 14,
                InvoiceType = "SERVICE",
                Mechanic = new Mechanic { Id = 1, Name = "Jose", IsActive = true },
                Vehicle = new Vehicle { Id = 3, Plate = "BXR42H", Model = "Boxer 150 2021" },
                CreatedAt = today.AddHours(9).AddMinutes(45),
                LaborAmount = 65000m,
                TotalAmount = 209100m,
                Items =
                [
                    new InvoiceItem { Id = 51, Description = "Tornillo Leva", Quantity = 1, UnitPrice = 3900m, Subtotal = 3900m },
                    new InvoiceItem { Id = 52, Description = "Pastillas de freno delantero", Quantity = 2, UnitPrice = 18500m, Subtotal = 37000m },
                    new InvoiceItem { Id = 53, Description = "Aceite Motor 4T 20W-50", Quantity = 1, UnitPrice = 28200m, Subtotal = 28200m },
                    new InvoiceItem { Id = 54, Description = "Filtro de aceite", Quantity = 1, UnitPrice = 15000m, Subtotal = 15000m },
                ],
            },
            new Invoice
            {
                Id = 13,
                InvoiceType = "DELIVERY",
                BuyerName = "Talleres La 80",
                CreatedAt = today.AddHours(9).AddMinutes(22),
                TotalAmount = 37000m,
                Items =
                [
                    new InvoiceItem { Id = 49, Description = "Suzuki 150 Palanca de Freno", Quantity = 2, UnitPrice = 18500m, Subtotal = 37000m },
                ],
            },
            new Invoice
            {
                Id = 12,
                InvoiceType = "SERVICE",
                Mechanic = new Mechanic { Id = 2, Name = "Carlos", IsActive = true },
                Vehicle = new Vehicle { Id = 5, Plate = "KDP93F", Model = "Pulsar NS 200 2023" },
                CreatedAt = today.AddHours(8).AddMinutes(55),
                LaborAmount = 45000m,
                TotalAmount = 156400m,
                Items =
                [
                    new InvoiceItem { Id = 46, Description = "Kit de arrastre completo", Quantity = 1, UnitPrice = 85000m, Subtotal = 85000m },
                    new InvoiceItem { Id = 47, Description = "Lubricante cadena", Quantity = 1, UnitPrice = 12400m, Subtotal = 12400m },
                    new InvoiceItem { Id = 48, Description = "Tensor de cadena", Quantity = 1, UnitPrice = 14000m, Subtotal = 14000m },
                ],
            },
            new Invoice
            {
                Id = 11,
                InvoiceType = "SERVICE",
                Mechanic = new Mechanic { Id = 3, Name = "Pedro", IsActive = true },
                Vehicle = new Vehicle { Id = 7, Plate = "MNQ17E", Model = "NKD 125 2020" },
                CreatedAt = today.AddHours(8).AddMinutes(30),
                LaborAmount = 30000m,
                TotalAmount = 89500m,
                Items =
                [
                    new InvoiceItem { Id = 44, Description = "Bujía NGK CR7HSA", Quantity = 1, UnitPrice = 12500m, Subtotal = 12500m },
                    new InvoiceItem { Id = 45, Description = "Cable de embrague", Quantity = 1, UnitPrice = 47000m, Subtotal = 47000m },
                ],
            },
            new Invoice
            {
                Id = 10,
                InvoiceType = "DELIVERY",
                BuyerName = "Motos del Sur",
                CreatedAt = today.AddHours(8).AddMinutes(10),
                TotalAmount = 54200m,
                Items =
                [
                    new InvoiceItem { Id = 42, Description = "Espejo retrovisor derecho universal", Quantity = 2, UnitPrice = 15600m, Subtotal = 31200m },
                    new InvoiceItem { Id = 43, Description = "Manigueta derecha", Quantity = 1, UnitPrice = 23000m, Subtotal = 23000m },
                ],
            },
            new Invoice
            {
                Id = 9,
                InvoiceType = "SERVICE",
                Mechanic = new Mechanic { Id = 4, Name = "Miguel", IsActive = true },
                Vehicle = new Vehicle { Id = 9, Plate = "TPL28C", Model = "Crypton FI 115 2022" },
                CreatedAt = today.AddHours(7).AddMinutes(50),
                LaborAmount = 55000m,
                TotalAmount = 178300m,
                Items =
                [
                    new InvoiceItem { Id = 39, Description = "Llanta trasera 275-17", Quantity = 1, UnitPrice = 62000m, Subtotal = 62000m },
                    new InvoiceItem { Id = 40, Description = "Rin trasero", Quantity = 1, UnitPrice = 45000m, Subtotal = 45000m },
                    new InvoiceItem { Id = 41, Description = "Mano de montaje", Quantity = 1, UnitPrice = 16300m, Subtotal = 16300m },
                ],
            },
            new Invoice
            {
                Id = 8,
                InvoiceType = "SERVICE",
                Mechanic = new Mechanic { Id = 1, Name = "Jose", IsActive = true },
                Vehicle = new Vehicle { Id = 11, Plate = "GHL54A", Model = "FZ 250 2024" },
                CreatedAt = today.AddHours(7).AddMinutes(15),
                LaborAmount = 80000m,
                TotalAmount = 322500m,
                Items =
                [
                    new InvoiceItem { Id = 36, Description = "Pastillas de freno trasero", Quantity = 1, UnitPrice = 22500m, Subtotal = 22500m },
                    new InvoiceItem { Id = 37, Description = "Disco de freno delantero", Quantity = 1, UnitPrice = 135000m, Subtotal = 135000m },
                    new InvoiceItem { Id = 38, Description = "Líquido de frenos DOT 4", Quantity = 2, UnitPrice = 42500m, Subtotal = 85000m },
                ],
            },
        ];
    }

    // ── Build clickable invoice rows ─────────────────────

    private void BuildInvoiceRows()
    {
        foreach (var invoice in _sampleInvoices)
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
                Text = invoice.Mechanic?.Name ?? "—",
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
                Text = FormatCurrency(invoice.TotalAmount),
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
            var tapGesture = new TapGestureRecognizer();
            var capturedInvoice = invoice;
            tapGesture.Tapped += (_, _) => OpenInvoiceViewer(capturedInvoice);
            grid.GestureRecognizers.Add(tapGesture);

            // Pointer cursor on hover
            var pointerGesture = new PointerGestureRecognizer();
            pointerGesture.PointerEntered += (_, _) => grid.BackgroundColor = Color.FromArgb("#F7F6F3");
            pointerGesture.PointerExited += (_, _) => grid.BackgroundColor = Colors.Transparent;
            grid.GestureRecognizers.Add(pointerGesture);

            InvoiceRowsContainer.Children.Add(grid);

            // Divider
            InvoiceRowsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current.Resources["Border"],
            });
        }
    }

    private static void OpenInvoiceViewer(Invoice invoice)
    {
        var page = new InvoiceViewerPage(invoice);
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

    private static string FormatCurrency(decimal value)
        => $"${value:N0}".Replace(",", ".");
}
