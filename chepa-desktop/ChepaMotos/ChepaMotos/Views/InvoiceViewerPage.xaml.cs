using ChepaMotos.Models;

namespace ChepaMotos.Views;

public partial class InvoiceViewerPage : ContentPage
{
    public InvoiceViewerPage(Invoice invoice)
    {
        InitializeComponent();
        LoadInvoice(invoice);
    }

    private void LoadInvoice(Invoice invoice)
    {
        // Invoice number
        InvoiceNumberLabel.Text = $"Factura #{invoice.Id:D3}";

        // Type badge
        var isService = invoice.InvoiceType == "SERVICE";
        if (isService)
        {
            TypeBadge.BackgroundColor = (Color)Application.Current!.Resources["AccentLight"];
            TypeBadgeLabel.Text = "Servicio";
            TypeBadgeLabel.TextColor = (Color)Application.Current.Resources["Accent"];
        }
        else
        {
            TypeBadge.BackgroundColor = (Color)Application.Current!.Resources["BlueLight"];
            TypeBadgeLabel.Text = "Venta";
            TypeBadgeLabel.TextColor = (Color)Application.Current.Resources["Blue"];
        }

        // Status badge
        if (invoice.IsCancelled)
        {
            StatusBadge.IsVisible = true;
            StatusBadge.BackgroundColor = Color.FromArgb("#FCE8E8");
            StatusBadgeLabel.Text = "Anulada";
            StatusBadgeLabel.TextColor = Color.FromArgb("#C0392B");
        }

        // Fields based on type
        if (isService)
        {
            ServiceFieldsGrid.IsVisible = true;
            DeliveryFieldsGrid.IsVisible = false;

            ServiceDateLabel.Text = invoice.CreatedAt.ToString("dd/MM/yyyy");
            MechanicLabel.Text = invoice.Mechanic?.Name ?? "—";
            PlateLabel.Text = invoice.Vehicle?.Plate ?? "—";
            ModelLabel.Text = invoice.Vehicle?.Model ?? "—";

            // Labor
            LaborRow.IsVisible = true;
            LaborLabel.Text = FormatCurrency(invoice.LaborAmount);
        }
        else
        {
            ServiceFieldsGrid.IsVisible = false;
            DeliveryFieldsGrid.IsVisible = true;

            DeliveryDateLabel.Text = invoice.CreatedAt.ToString("dd/MM/yyyy");
            BuyerLabel.Text = invoice.BuyerName ?? "—";

            LaborRow.IsVisible = false;
        }

        // Items
        BuildItemRows(invoice.Items);

        // Total
        TotalLabel.Text = FormatCurrency(invoice.TotalAmount);
    }

    private void BuildItemRows(List<InvoiceItem> items)
    {
        foreach (var item in items)
        {
            var row = new VerticalStackLayout { Spacing = 0 };

            var grid = new Grid
            {
                ColumnDefinitions =
                [
                    new ColumnDefinition(new GridLength(60)),
                    new ColumnDefinition(GridLength.Star),
                    new ColumnDefinition(new GridLength(110)),
                    new ColumnDefinition(new GridLength(110)),
                ],
                Padding = new Thickness(8, 10),
                BackgroundColor = (Color)Application.Current!.Resources["Surface"],
            };

            var qtyLabel = new Label
            {
                Text = item.Quantity % 1 == 0 ? $"{(int)item.Quantity}" : $"{item.Quantity}",
                FontFamily = "IBMPlexMono",
                FontSize = 13,
                TextColor = (Color)Application.Current.Resources["TextPrimary"],
                HorizontalTextAlignment = TextAlignment.Center,
                VerticalTextAlignment = TextAlignment.Center,
            };
            Grid.SetColumn(qtyLabel, 0);

            var descLabel = new Label
            {
                Text = item.Description,
                FontFamily = "IBMPlexSans",
                FontSize = 13,
                TextColor = (Color)Application.Current.Resources["TextPrimary"],
                VerticalTextAlignment = TextAlignment.Center,
            };
            Grid.SetColumn(descLabel, 1);

            var priceLabel = new Label
            {
                Text = FormatCurrency(item.UnitPrice),
                FontFamily = "IBMPlexMono",
                FontSize = 13,
                TextColor = (Color)Application.Current.Resources["TextPrimary"],
                HorizontalTextAlignment = TextAlignment.End,
                VerticalTextAlignment = TextAlignment.Center,
            };
            Grid.SetColumn(priceLabel, 2);

            var subtotalLabel = new Label
            {
                Text = FormatCurrency(item.Subtotal),
                FontFamily = "IBMPlexMono",
                FontSize = 13,
                TextColor = (Color)Application.Current.Resources["TextPrimary"],
                HorizontalTextAlignment = TextAlignment.End,
                VerticalTextAlignment = TextAlignment.Center,
            };
            Grid.SetColumn(subtotalLabel, 3);

            grid.Children.Add(qtyLabel);
            grid.Children.Add(descLabel);
            grid.Children.Add(priceLabel);
            grid.Children.Add(subtotalLabel);

            row.Children.Add(grid);
            row.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current.Resources["Border"],
            });

            ItemsContainer.Children.Add(row);
        }
    }

    private void OnCloseClicked(object? sender, EventArgs e)
    {
        if (Window is Window window)
            Application.Current?.CloseWindow(window);
    }

    private static string FormatCurrency(decimal value)
        => $"${value:N0}".Replace(",", ".");
}
