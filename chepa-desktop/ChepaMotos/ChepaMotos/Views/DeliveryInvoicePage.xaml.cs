using System.Collections.ObjectModel;
using ChepaMotos.Behaviors;
using ChepaMotos.Helpers;
using ChepaMotos.Services;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class DeliveryInvoicePage : ContentPage
{
    private readonly ObservableCollection<InvoiceItemRow> _items = [];

    public DeliveryInvoicePage()
    {
        InitializeComponent();

        // Start with one empty row
        AddItem(new InvoiceItemRow());

        BindableLayout.SetItemsSource(ItemsContainer, _items);

        // Clear validation errors on interaction
        BuyerEntry.TextChanged += (_, _) => ClearFieldError(BuyerBorder, BuyerError);

        RecalculateTotal();
    }

    private async void OnCancelClicked(object? sender, EventArgs e)
    {
        bool confirm = await DisplayAlertAsync(
            "Cancelar factura",
            "¿Estás seguro de que deseas cancelar? Se perderán los datos ingresados.",
            "Sí, cancelar",
            "Volver");

        if (confirm && Window is Window window)
            Application.Current?.CloseWindow(window);
    }

    /// <summary>Fired after a delivery invoice is successfully confirmed.</summary>
    public event Action? InvoiceConfirmed;

    private void OnConfirmClicked(object? sender, EventArgs e)
    {
        if (!ValidateForm())
            return;

        // TODO: [API] Replace with: await InvoiceService.CreateDeliveryInvoice(request)
        // Maps to: POST /invoices/delivery
        var buyerName = BuyerEntry.Text?.Trim() ?? "";
        var invoiceDate = DeliveryDatePicker.Date ?? DateTime.Today;

        var items = _items
            .Where(i => !string.IsNullOrWhiteSpace(i.Description) && i.Subtotal > 0)
            .Select(i => (i.Description.Trim(), ParseItemQuantity(i.Quantity), CurrencyInputBehavior.GetValue(i.UnitPrice)))
            .ToList();

        MockDataService.AddDeliveryInvoice(buyerName, invoiceDate, items);

        InvoiceConfirmed?.Invoke();

        if (Window is Window window)
            Application.Current?.CloseWindow(window);
    }

    private static decimal ParseItemQuantity(string? text)
    {
        return NumericInputParser.ParseDecimal(text);
    }

    // ── Form validation ──────────────────────────────────

    private bool ValidateForm()
    {
        var isValid = true;

        // Buyer
        var buyer = BuyerEntry.Text?.Trim() ?? "";
        if (string.IsNullOrEmpty(buyer))
        {
            SetFieldError(BuyerBorder, BuyerError, "Ingresa el nombre del comprador");
            isValid = false;
        }

        // Items — need at least one with description and price
        var validItems = _items.Where(i =>
            !string.IsNullOrWhiteSpace(i.Description) && i.Subtotal > 0).ToList();

        if (validItems.Count == 0)
        {
            ItemsError.Text = "Agrega al menos un ítem con descripción y precio";
            ItemsError.IsVisible = true;
            isValid = false;
        }
        else
        {
            ItemsError.IsVisible = false;
        }

        return isValid;
    }

    private static void SetFieldError(Border border, Label errorLabel, string message)
    {
        border.Stroke = new SolidColorBrush(Color.FromArgb("#C13B0A"));
        errorLabel.Text = message;
        errorLabel.IsVisible = true;
    }

    private static void ClearFieldError(Border border, Label errorLabel)
    {
        if (!errorLabel.IsVisible) return;
        border.Stroke = new SolidColorBrush(Color.FromArgb("#D8D5CC"));
        errorLabel.IsVisible = false;
    }

    // ── Item row management ──────────────────────────────

    private void AddItem(InvoiceItemRow item)
    {
        item.SubtotalChanged += (_, _) => RecalculateTotal();
        _items.Add(item);
    }

    private void OnAddItemClicked(object? sender, EventArgs e)
    {
        AddItem(new InvoiceItemRow());
    }

    private void OnDeleteItemClicked(object? sender, EventArgs e)
    {
        if (sender is Button btn && btn.CommandParameter is InvoiceItemRow item)
        {
            _items.Remove(item);
            RecalculateTotal();
        }
    }

    private void RecalculateTotal()
    {
        var total = _items.Sum(i => i.Subtotal);
        TotalLabel.Text = $"${total:N0}".Replace(",", ".");
    }
}
