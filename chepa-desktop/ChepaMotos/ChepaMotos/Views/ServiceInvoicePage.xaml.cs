using System.Collections.ObjectModel;
using ChepaMotos.Behaviors;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class ServiceInvoicePage : ContentPage
{
    private readonly ObservableCollection<InvoiceItemRow> _items = [];

    public ServiceInvoicePage()
    {
        InitializeComponent();

        // Seed with placeholder data
        AddItem(new InvoiceItemRow { Quantity = "1", Description = "Tornillo Leva", UnitPrice = "3.900" });
        AddItem(new InvoiceItemRow { Quantity = "2", Description = "Pastillas de freno", UnitPrice = "18.500" });
        AddItem(new InvoiceItemRow()); // empty row ready for input

        BindableLayout.SetItemsSource(ItemsContainer, _items);

        // Recalculate total when labor changes
        LaborEntry.TextChanged += (_, _) => RecalculateTotal();
        RecalculateTotal();
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        if (Window is Window window)
            window.Destroying += OnWindowClosing;
    }

    protected override void OnDisappearing()
    {
        base.OnDisappearing();
        if (Window is Window window)
            window.Destroying -= OnWindowClosing;
    }

    private async void OnWindowClosing(object? sender, EventArgs e)
    {
        // The Destroying event fires after the close is already committed,
        // so we can't cancel it. Instead we handle close via the cancel button.
        // This is kept as a hook for future use.
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
        // For now, just close
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
        var itemsTotal = _items.Sum(i => i.Subtotal);
        var labor = CurrencyInputBehavior.GetValue(LaborEntry.Text);
        var total = itemsTotal + labor;
        TotalLabel.Text = $"${total:N0}".Replace(",", ".");
    }

    // ── Model auto-fill ──────────────────────────────────

    /// <summary>
    /// Called by plate lookup when a matching vehicle is found.
    /// Sets the model field to auto-fill (blue) state and shows the edit button.
    /// </summary>
    public void SetModelAutoFilled(string model)
    {
        ModelEntry.Text = model;
        ModelEntry.IsReadOnly = true;
        ModelEntry.TextColor = Color.FromArgb("#1A4A82");          // Blue
        ModelFieldBorder.BackgroundColor = Color.FromArgb("#DDE8F5"); // BlueLight
        ModelFieldBorder.Stroke = new SolidColorBrush(Color.FromArgb("#A8C0DC"));
        ModelEditButton.IsVisible = true;
    }

    /// <summary>
    /// User taps the ✎ edit button — clears auto-fill state so they can type freely.
    /// </summary>
    private void OnModelEditClicked(object? sender, EventArgs e)
    {
        ModelEntry.IsReadOnly = false;
        ModelEntry.TextColor = (Color)Application.Current!.Resources["TextPrimary"];
        ModelFieldBorder.BackgroundColor = (Color)Application.Current.Resources["Surface"];
        ModelFieldBorder.Stroke = new SolidColorBrush((Color)Application.Current.Resources["Border"]);
        ModelEditButton.IsVisible = false;
        ModelEntry.Focus();
    }
}
