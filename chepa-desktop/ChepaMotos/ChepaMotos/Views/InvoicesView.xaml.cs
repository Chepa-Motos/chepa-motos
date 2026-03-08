using ChepaMotos.Models;
using ChepaMotos.Services;

namespace ChepaMotos.Views;

public partial class InvoicesView : ContentView
{
    private string _activeFilter = "Todas";

    public InvoicesView()
    {
        InitializeComponent();
        FilterDatePicker.Date = DateTime.Today;
        LoadInvoices();
    }

    private void OnDateSelected(object? sender, DateChangedEventArgs e)
    {
        LoadInvoices();
    }

    private void OnFilterTapped(object? sender, TappedEventArgs e)
    {
        if (e.Parameter is string filter)
        {
            _activeFilter = filter;
            UpdateFilterVisuals();
            LoadInvoices();
        }
    }

    private void UpdateFilterVisuals()
    {
        var filters = new Dictionary<string, Border>
        {
            { "Todas", FilterAll },
            { "Servicio", FilterService },
            { "Venta", FilterDelivery },
            { "Anuladas", FilterCancelled }
        };

        foreach (var kvp in filters)
        {
            bool isActive = kvp.Key == _activeFilter;
            kvp.Value.Stroke = isActive
                ? (Color)Application.Current!.Resources["BorderStrong"]
                : (Color)Application.Current!.Resources["Border"];

            var label = (Label)kvp.Value.Content!;
            label.TextColor = isActive
                ? (Color)Application.Current!.Resources["TextPrimary"]
                : (Color)Application.Current!.Resources["TextSecondary"];
        }
    }

    private void LoadInvoices()
    {
        var date = FilterDatePicker.Date;

        List<Invoice> invoices;
        if (_activeFilter == "Anuladas")
        {
            // TODO: [API] Replace with: var invoices = await InvoiceService.GetInvoices(date, cancelled: true)
            // Maps to: GET /invoices?date=YYYY-MM-DD&cancelled=true
            invoices = MockDataService.GetInvoices(date: date, cancelled: true);
        }
        else
        {
            string? type = _activeFilter switch
            {
                "Servicio" => "SERVICE",
                "Venta" => "DELIVERY",
                _ => null
            };
            // TODO: [API] Replace with: var invoices = await InvoiceService.GetInvoices(date, type)
            // Maps to: GET /invoices?date=YYYY-MM-DD&type=SERVICE|DELIVERY
            invoices = MockDataService.GetInvoices(date: date, type: type);
        }

        // Sort by newest first
        invoices = invoices.OrderByDescending(i => i.CreatedAt).ToList();

        ResultCountLabel.Text = $"{invoices.Count} resultado{(invoices.Count != 1 ? "s" : "")}";
        EmptyLabel.IsVisible = invoices.Count == 0;

        InvoiceRowsContainer.Children.Clear();

        foreach (var inv in invoices)
        {
            var row = BuildInvoiceRow(inv);
            InvoiceRowsContainer.Children.Add(row);
            InvoiceRowsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current!.Resources["Border"]
            });
        }
    }

    private View BuildInvoiceRow(Invoice inv)
    {
        var grid = new Grid
        {
            ColumnDefinitions =
            {
                new ColumnDefinition(new GridLength(60)),
                new ColumnDefinition(new GridLength(80)),
                new ColumnDefinition(new GridLength(120)),
                new ColumnDefinition(new GridLength(120)),
                new ColumnDefinition(new GridLength(100)),
                new ColumnDefinition(new GridLength(100)),
                new ColumnDefinition(new GridLength(80)),
                new ColumnDefinition(new GridLength(80))
            },
            Padding = new Thickness(16, 10)
        };

        if (inv.IsCancelled) grid.Opacity = 0.5;

        // # column
        var idLabel = new Label
        {
            Text = inv.Id.ToString("D3"),
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"],
            VerticalTextAlignment = TextAlignment.Center
        };
        Grid.SetColumn(idLabel, 0);
        grid.Children.Add(idLabel);

        // Type badge
        bool isService = inv.InvoiceType == "SERVICE";
        var typeBadge = new Border
        {
            StrokeShape = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 4 },
            StrokeThickness = 0,
            BackgroundColor = isService
                ? (Color)Application.Current!.Resources["AccentLight"]
                : (Color)Application.Current!.Resources["BlueLight"],
            Padding = new Thickness(8, 2),
            VerticalOptions = LayoutOptions.Center,
            HorizontalOptions = LayoutOptions.Start,
            Content = new Label
            {
                Text = isService ? "Servicio" : "Venta",
                FontFamily = "IBMPlexSansMedium",
                FontSize = 11,
                TextColor = isService
                    ? (Color)Application.Current!.Resources["Accent"]
                    : (Color)Application.Current!.Resources["Blue"]
            }
        };
        Grid.SetColumn(typeBadge, 1);
        grid.Children.Add(typeBadge);

        // Plate
        if (inv.Vehicle != null)
        {
            var plateBadge = new Border
            {
                StrokeShape = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 4 },
                StrokeThickness = 1,
                Stroke = (Color)Application.Current!.Resources["Border"],
                BackgroundColor = (Color)Application.Current!.Resources["Surface2"],
                Padding = new Thickness(8, 2),
                VerticalOptions = LayoutOptions.Center,
                HorizontalOptions = LayoutOptions.Start,
                Content = new Label
                {
                    Text = inv.Vehicle.Plate,
                    FontFamily = "IBMPlexMono",
                    FontSize = 11,
                    TextColor = (Color)Application.Current!.Resources["TextPrimary"]
                }
            };
            Grid.SetColumn(plateBadge, 2);
            grid.Children.Add(plateBadge);
        }
        else
        {
            var dash = new Label
            {
                Text = "—",
                FontFamily = "IBMPlexSans",
                FontSize = 13,
                TextColor = (Color)Application.Current!.Resources["TextMuted"],
                VerticalTextAlignment = TextAlignment.Center
            };
            Grid.SetColumn(dash, 2);
            grid.Children.Add(dash);
        }

        // Buyer
        var buyerLabel = new Label
        {
            Text = inv.BuyerName ?? "—",
            FontFamily = "IBMPlexSans",
            FontSize = 13,
            TextColor = inv.BuyerName != null
                ? (Color)Application.Current!.Resources["TextPrimary"]
                : (Color)Application.Current!.Resources["TextMuted"],
            VerticalTextAlignment = TextAlignment.Center,
            LineBreakMode = LineBreakMode.TailTruncation
        };
        Grid.SetColumn(buyerLabel, 3);
        grid.Children.Add(buyerLabel);

        // Mechanic
        var mechanicLabel = new Label
        {
            Text = inv.Mechanic?.Name?.Split(' ')[0] ?? "—",
            FontFamily = "IBMPlexSans",
            FontSize = 13,
            TextColor = inv.Mechanic != null
                ? (Color)Application.Current!.Resources["TextPrimary"]
                : (Color)Application.Current!.Resources["TextMuted"],
            VerticalTextAlignment = TextAlignment.Center
        };
        Grid.SetColumn(mechanicLabel, 4);
        grid.Children.Add(mechanicLabel);

        // Total
        var totalLabel = new Label
        {
            Text = MockDataService.FormatCurrency(inv.TotalAmount),
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"],
            VerticalTextAlignment = TextAlignment.Center,
            HorizontalTextAlignment = TextAlignment.End
        };
        Grid.SetColumn(totalLabel, 5);
        grid.Children.Add(totalLabel);

        // Status badge
        Color statusBg, statusText;
        string statusLabel;
        if (inv.IsCancelled)
        {
            statusBg = (Color)Application.Current!.Resources["AnuladaBg"];
            statusText = (Color)Application.Current!.Resources["AnuladaText"];
            statusLabel = "Anulada";
        }
        else
        {
            statusBg = (Color)Application.Current!.Resources["GreenLight"];
            statusText = (Color)Application.Current!.Resources["Green"];
            statusLabel = "Activa";
        }
        var statusBadge = new Border
        {
            StrokeShape = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 4 },
            StrokeThickness = 0,
            BackgroundColor = statusBg,
            Padding = new Thickness(8, 2),
            VerticalOptions = LayoutOptions.Center,
            HorizontalOptions = LayoutOptions.Start,
            Content = new Label
            {
                Text = statusLabel,
                FontFamily = "IBMPlexSansMedium",
                FontSize = 11,
                TextColor = statusText
            }
        };
        Grid.SetColumn(statusBadge, 6);
        grid.Children.Add(statusBadge);

        // Time
        var timeLabel = new Label
        {
            Text = inv.CreatedAt.ToString("HH:mm"),
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextMuted"],
            VerticalTextAlignment = TextAlignment.Center,
            HorizontalTextAlignment = TextAlignment.End
        };
        Grid.SetColumn(timeLabel, 7);
        grid.Children.Add(timeLabel);

        // Tap to open invoice viewer
        var tap = new TapGestureRecognizer();
        tap.Tapped += (s, args) => OpenInvoiceViewer(inv);
        grid.GestureRecognizers.Add(tap);

        return grid;
    }

    private void OpenInvoiceViewer(Invoice invoice)
    {
        var viewer = new InvoiceViewerPage(invoice);
        viewer.InvoiceCancelled += () => MainThread.BeginInvokeOnMainThread(LoadInvoices);
        var window = new Window(viewer)
        {
            Title = $"Factura #{invoice.Id:D3}",
            Width = 720,
            Height = 680
        };
        Application.Current?.OpenWindow(window);
    }
}
