using System.Collections.ObjectModel;
using System.Text.RegularExpressions;
using ChepaMotos.Behaviors;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class ServiceInvoicePage : ContentPage
{
    private readonly ObservableCollection<InvoiceItemRow> _items = [];
    private static readonly Regex PlateRegex = new(@"^[A-Z]{3}[0-9]{2}[A-Z]$", RegexOptions.Compiled);

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

        // Clear validation errors on interaction
        MechanicPicker.SelectedIndexChanged += (_, _) => ClearFieldError(MechanicBorder, MechanicError);
        ModelEntry.TextChanged += (_, _) => ClearFieldError(ModelFieldBorder, ModelError);

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
        if (!ValidateForm())
            return;

        // Will call service layer in the future
        // For now, just close
        if (Window is Window window)
            Application.Current?.CloseWindow(window);
    }

    // ── Plate handling ───────────────────────────────────

    private bool _isUpdatingPlate;

    private void OnPlateTextChanged(object? sender, TextChangedEventArgs e)
    {
        if (_isUpdatingPlate) return;

        // Auto-uppercase
        var upper = e.NewTextValue?.ToUpperInvariant()?.Replace(" ", "")?.Replace("-", "") ?? "";
        if (upper != e.NewTextValue)
        {
            _isUpdatingPlate = true;
            PlateEntry.Text = upper;
            _isUpdatingPlate = false;
        }

        // Clear validation error on typing
        ClearFieldError(PlateBorder, PlateError);

        // Warning for uncommon format (only when 6 chars — full plate length)
        if (upper.Length >= 6)
            PlateWarning.IsVisible = !PlateRegex.IsMatch(upper);
        else
            PlateWarning.IsVisible = false;
    }

    // ── Form validation ──────────────────────────────────

    private bool ValidateForm()
    {
        var isValid = true;

        // Mechanic
        if (MechanicPicker.SelectedIndex < 0)
        {
            SetFieldError(MechanicBorder, MechanicError, "Selecciona un mecánico");
            isValid = false;
        }

        // Plate
        var plate = PlateEntry.Text?.Trim() ?? "";
        if (string.IsNullOrEmpty(plate))
        {
            SetFieldError(PlateBorder, PlateError, "Ingresa la placa del vehículo");
            isValid = false;
        }

        // Model
        var model = ModelEntry.Text?.Trim() ?? "";
        if (string.IsNullOrEmpty(model))
        {
            SetFieldError(ModelFieldBorder, ModelError, "Ingresa el modelo de la moto");
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
