using System.Collections.ObjectModel;
using ChepaMotos.Behaviors;
using ChepaMotos.Helpers;
using ChepaMotos.Models;
using ChepaMotos.Models.Requests;
using ChepaMotos.Services.Domain;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class DeliveryInvoicePage : ContentPage
{
    private readonly IInvoiceService _invoiceService;
    private readonly ObservableCollection<InvoiceItemRow> _items = [];
    private bool _isConfirming;

    public DeliveryInvoicePage(IInvoiceService invoiceService)
    {
        _invoiceService = invoiceService;
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
        if (_isConfirming) return;

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

    private async void OnConfirmClicked(object? sender, EventArgs e)
    {
        if (_isConfirming) return;
        if (!ValidateForm()) return;

        // POST /invoices/delivery. El backend marca created_at con la hora del servidor;
        // DeliveryDatePicker es solo informativo y se ignora aquí.
        var request = new CreateDeliveryInvoiceRequest
        {
            BuyerName = BuyerEntry.Text?.Trim() ?? string.Empty,
            Items = _items
                .Where(i => !string.IsNullOrWhiteSpace(i.Description) && i.Subtotal > 0)
                .Select(i => new CreateInvoiceItemRequest
                {
                    Description = i.Description.Trim(),
                    Quantity = ParseItemQuantity(i.Quantity),
                    UnitPrice = CurrencyInputBehavior.GetValue(i.UnitPrice),
                })
                .ToList(),
        };

        _isConfirming = true;
        ConfirmButton.IsEnabled = false;
        ConfirmButton.Text = "Guardando…";
        CancelButton.IsEnabled = false;

        try
        {
            await _invoiceService.CreateDeliveryAsync(request);
            InvoiceConfirmed?.Invoke();
            if (Window is Window window)
                Application.Current?.CloseWindow(window);
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.ValidationError)
        {
            if (!TryApplyValidationError(ex.Message))
                await DisplayAlertAsync("Validación", ex.Message, "Aceptar");
        }
        catch (ApiException ex)
        {
            await DisplayAlertAsync("No se pudo crear la factura", ex.Message, "Aceptar");
        }
        catch (HttpRequestException)
        {
            await DisplayAlertAsync(
                "Sin conexión",
                "No se pudo conectar al servidor. Inténtalo de nuevo.",
                "Aceptar");
        }
        catch (TaskCanceledException)
        {
            await DisplayAlertAsync("Tiempo agotado", "El servidor tardó demasiado en responder.", "Aceptar");
        }
        finally
        {
            _isConfirming = false;
            ConfirmButton.IsEnabled = true;
            ConfirmButton.Text = "Confirmar factura";
            CancelButton.IsEnabled = true;
        }
    }

    private bool TryApplyValidationError(string message)
    {
        var colon = message.IndexOf(':');
        if (colon <= 0) return false;

        var field = message[..colon].Trim();
        var rest = message[(colon + 1)..].Trim();

        switch (field)
        {
            case "buyer_name":
                SetFieldError(BuyerBorder, BuyerError, rest);
                return true;
            case "items":
                ItemsError.Text = rest;
                ItemsError.IsVisible = true;
                return true;
            default:
                return false;
        }
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
