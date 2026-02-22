using System.Collections.ObjectModel;
using ChepaMotos.Behaviors;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class DeliveryInvoicePage : ContentPage
{
    private readonly ObservableCollection<InvoiceItemRow> _items = [];

    public DeliveryInvoicePage()
    {
        InitializeComponent();

        // Seed with placeholder data
        AddItem(new InvoiceItemRow { Quantity = "2", Description = "Suzuki 150 Palanca de Freno", UnitPrice = "18.500" });
        AddItem(new InvoiceItemRow { Quantity = "1", Description = "Cable de embrague AKT", UnitPrice = "12.000" });
        AddItem(new InvoiceItemRow()); // empty row ready for input

        BindableLayout.SetItemsSource(ItemsContainer, _items);
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

    private void OnConfirmClicked(object? sender, EventArgs e)
    {
        // Will call service layer in the future
        if (Window is Window window)
            Application.Current?.CloseWindow(window);
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
            item.SubtotalChanged -= (_, _) => RecalculateTotal();
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
