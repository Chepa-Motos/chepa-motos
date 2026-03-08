using ChepaMotos.Models;
using ChepaMotos.Services;
using System.Diagnostics;

namespace ChepaMotos.Views;

public partial class InvoiceViewerPage : ContentPage
{
    private Invoice _invoice;

    /// <summary>
    /// Fired after an invoice is successfully cancelled, so parent views can refresh.
    /// </summary>
    public event Action? InvoiceCancelled;

    public InvoiceViewerPage(Invoice invoice)
    {
        InitializeComponent();
        _invoice = invoice;
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

        // Status badge + Anular button visibility
        if (invoice.IsCancelled)
        {
            ShowCancelledState();
        }
        else
        {
            CancelInvoiceButton.IsVisible = true;
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

    private async void OnCancelInvoiceClicked(object? sender, EventArgs e)
    {
        bool confirm = await DisplayAlertAsync(
            "Anular factura",
            $"¿Está seguro de que desea anular la Factura #{_invoice.Id:D3}?\n\nEsta acción no se puede deshacer.",
            "Sí, anular",
            "Cancelar");

        if (!confirm) return;

        // TODO: [API] Replace with: await InvoiceService.CancelInvoice(_invoice.Id)
        // Maps to: PATCH /invoices/{id}/cancel
        MockDataService.CancelInvoice(_invoice.Id);

        _invoice.IsCancelled = true;
        ShowCancelledState();
        CancelInvoiceButton.IsVisible = false;

        ToastService.ShowSuccess(this, $"Factura #{_invoice.Id:D3} anulada");
        InvoiceCancelled?.Invoke();
    }

    private void ShowCancelledState()
    {
        StatusBadge.IsVisible = true;
        StatusBadge.BackgroundColor = Color.FromArgb("#FCE8E8");
        StatusBadgeLabel.Text = "Anulada";
        StatusBadgeLabel.TextColor = Color.FromArgb("#C0392B");
    }

    private void OnCloseClicked(object? sender, EventArgs e)
    {
        if (Window is Window window)
            Application.Current?.CloseWindow(window);
    }

    // ── Print / Export PDF ───────────────────────────────

    private async void OnPrintClicked(object? sender, EventArgs e)
    {
        try
        {
            // Generate PDF to a temp file
            var tempPath = Path.Combine(Path.GetTempPath(), $"ChepaMotos_Factura_{_invoice.Id:D3}.pdf");
            InvoicePdfService.SaveReceiptPdf(_invoice, tempPath);

            // Try the "print" verb first (requires a PDF reader that supports it)
            try
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = tempPath,
                    Verb = "print",
                    UseShellExecute = true
                });
            }
            catch
            {
                // Fallback: open the PDF in the default viewer so the user can print from there
                Process.Start(new ProcessStartInfo
                {
                    FileName = tempPath,
                    UseShellExecute = true
                });

                await DisplayAlertAsync(
                    "Imprimir",
                    "Se abrió el PDF en el visor predeterminado. Usa Ctrl+P para imprimir desde ahí.",
                    "Entendido");
            }
        }
        catch (Exception ex)
        {
            await DisplayAlertAsync("Error", $"No se pudo imprimir: {ex.Message}", "Aceptar");
        }
    }

    private async void OnExportPdfClicked(object? sender, EventArgs e)
    {
        try
        {
            var typeLabel = _invoice.InvoiceType == "SERVICE" ? "Servicio" : "Venta";
            var defaultName = $"Factura_{_invoice.Id:D3}_{typeLabel}_{_invoice.CreatedAt:yyyyMMdd}.pdf";

            // Use the Documents folder as default save location
            var documentsPath = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
            var chepaFolder = Path.Combine(documentsPath, "ChepaMotos");
            Directory.CreateDirectory(chepaFolder);
            var filePath = Path.Combine(chepaFolder, defaultName);

            InvoicePdfService.SaveReceiptPdf(_invoice, filePath);

            ToastService.ShowSuccess(this, "PDF exportado correctamente");

            // Open the file in the default viewer
            Process.Start(new ProcessStartInfo
            {
                FileName = filePath,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            await DisplayAlertAsync("Error", $"No se pudo exportar: {ex.Message}", "Aceptar");
        }
    }

    private static string FormatCurrency(decimal value)
        => $"${value:N0}".Replace(",", ".");
}
