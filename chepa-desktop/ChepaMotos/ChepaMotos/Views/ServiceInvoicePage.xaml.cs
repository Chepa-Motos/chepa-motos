using System.Collections.ObjectModel;
using System.Text.RegularExpressions;
using ChepaMotos.Behaviors;
using ChepaMotos.Helpers;
using ChepaMotos.Models;
using ChepaMotos.Services;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class ServiceInvoicePage : ContentPage
{
    private readonly ObservableCollection<InvoiceItemRow> _items = [];
    private static readonly Regex PlateRegex = new(@"^[A-Z]{3}[0-9]{2}[A-Z]$", RegexOptions.Compiled);
    private List<Mechanic> _mechanics = [];

    // Autocomplete state
    private CancellationTokenSource? _debounceCts;
    private CancellationTokenSource? _modelDebounceCts;
    private InvoiceItemRow? _activeItemRow;
    private Entry? _activeDescriptionEntry;
    private bool _isSelectingSuggestion;
    private bool _isSelectingModelSuggestion;
    private int _selectedSuggestionIndex = -1;
    private int _selectedModelSuggestionIndex = -1;
    private readonly List<Grid> _suggestionRows = [];
    private readonly List<Grid> _modelSuggestionRows = [];
    private readonly List<ItemSuggestion> _currentSuggestions = [];
    private readonly List<string> _currentModelSuggestions = [];
    private readonly HashSet<object> _hookedNativeViews = [];
    private readonly Dictionary<InvoiceItemRow, object> _rowNativeTextBoxes = [];

    public ServiceInvoicePage()
    {
        InitializeComponent();

        // Load active mechanics into picker
        _mechanics = MockDataService.GetMechanics(activeOnly: true);
        MechanicPicker.ItemsSource = _mechanics.Select(m => m.Name).ToList();

        // Start with one empty row
        AddItem(new InvoiceItemRow());

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
        CancelPendingAutocomplete();
        CancelPendingModelAutocomplete();
        DetachNativeKeyHandlers();
        if (Window is Window window)
            window.Destroying -= OnWindowClosing;
    }

    private void OnWindowClosing(object? sender, EventArgs e)
    {
        // THIS IS AN ASYNC METHOD - async keyword removed to avoid compiler error
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
        {
            CancelPendingAutocomplete();
            CancelPendingModelAutocomplete();
            Application.Current?.CloseWindow(window);
        }
    }

    /// <summary>Fired after a service invoice is successfully confirmed.</summary>
    public event Action? InvoiceConfirmed;

    private void OnConfirmClicked(object? sender, EventArgs e)
    {
        if (!ValidateForm())
            return;

        // TODO: [API] Replace with: await InvoiceService.CreateServiceInvoice(request)
        // Maps to: POST /invoices/service
        var mechanicId = _mechanics[MechanicPicker.SelectedIndex].Id;
        var plate = PlateEntry.Text?.Trim().ToUpperInvariant() ?? "";
        var model = ModelEntry.Text?.Trim() ?? "";
        var labor = CurrencyInputBehavior.GetValue(LaborEntry.Text);
        var invoiceDate = ServiceDatePicker.Date ?? DateTime.Today;

        var items = _items
            .Where(i => !string.IsNullOrWhiteSpace(i.Description) && i.Subtotal > 0)
            .Select(i => (i.Description.Trim(), ParseItemQuantity(i.Quantity), CurrencyInputBehavior.GetValue(i.UnitPrice)))
            .ToList();

        MockDataService.AddServiceInvoice(mechanicId, plate, model, labor, invoiceDate, items);

        InvoiceConfirmed?.Invoke();

        if (Window is Window window)
        {
            CancelPendingAutocomplete();
            CancelPendingModelAutocomplete();
            Application.Current?.CloseWindow(window);
        }
    }

    private static decimal ParseItemQuantity(string? text)
    {
        return NumericInputParser.ParseDecimal(text);
    }

    private static void CancelPendingAutocomplete(ref CancellationTokenSource? cts)
    {
        cts?.Cancel();
        cts?.Dispose();
        cts = null;
    }

    private void CancelPendingAutocomplete()
    {
        CancelPendingAutocomplete(ref _debounceCts);
    }

    private void CancelPendingModelAutocomplete()
    {
        CancelPendingAutocomplete(ref _modelDebounceCts);
    }

    private void RunDebouncedSuggestions<T>(
        ref CancellationTokenSource? cts,
        Func<bool> shouldSkip,
        Func<List<T>> fetch,
        Action<List<T>> show)
    {
        CancelPendingAutocomplete(ref cts);
        cts = new CancellationTokenSource();
        var token = cts.Token;

        _ = Task.Run(async () =>
        {
            await Task.Delay(300, token);
            if (token.IsCancellationRequested || shouldSkip()) return;

            var suggestions = fetch();

            MainThread.BeginInvokeOnMainThread(() =>
            {
                if (token.IsCancellationRequested || shouldSkip()) return;
                show(suggestions);
            });
        }, token);
    }

    private static void ResetSuggestionState<T>(
        VerticalStackLayout container,
        List<Grid> rows,
        List<T> suggestions,
        ref int selectedIndex)
    {
        container.Children.Clear();
        rows.Clear();
        suggestions.Clear();
        selectedIndex = -1;
    }

    private static void MoveSelection(List<Grid> rows, ref int selectedIndex, int delta)
    {
        if (rows.Count == 0) return;

        if (selectedIndex >= 0 && selectedIndex < rows.Count)
            rows[selectedIndex].BackgroundColor = Colors.Transparent;

        selectedIndex += delta;

        if (selectedIndex >= rows.Count)
            selectedIndex = 0;
        if (selectedIndex < 0)
            selectedIndex = rows.Count - 1;

        rows[selectedIndex].BackgroundColor = Color.FromArgb("#F7F6F3");
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

    private void OnAddItemClicked(object? sender, EventArgs e)
    {
        AddItem(new InvoiceItemRow());
    }

    private void OnDeleteItemClicked(object? sender, EventArgs e)
    {
        if (sender is Button btn && btn.CommandParameter is InvoiceItemRow item)
        {
            DetachRowNativeKeyHandler(item);
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

        // Trigger fresh suggestions if the user is editing an existing model value.
        TriggerModelSuggestions(ModelEntry.Text);
    }

    private void OnModelEntryFocused(object? sender, FocusEventArgs e)
    {
        if (sender is Entry entry)
            HookNativeKeyHandler(entry);

        if (ModelEntry.IsReadOnly)
            return;

        TriggerModelSuggestions(ModelEntry.Text);
    }

    private void OnModelEntryCompleted(object? sender, EventArgs e)
    {
        if (ModelSuggestionsPopup.IsVisible && _currentModelSuggestions.Count > 0)
        {
            var index = _selectedModelSuggestionIndex >= 0 ? _selectedModelSuggestionIndex : 0;
            SelectModelSuggestionByKeyboard(index);
        }
        else
            HideModelSuggestions();
    }

    private void OnModelEntryTextChanged(object? sender, TextChangedEventArgs e)
    {
        if (_isSelectingModelSuggestion || ModelEntry.IsReadOnly)
        {
            HideModelSuggestions();
            return;
        }

        TriggerModelSuggestions(e.NewTextValue);
    }

    private void TriggerModelSuggestions(string? text)
    {
        if (ModelEntry.IsReadOnly)
        {
            HideModelSuggestions();
            return;
        }

        var query = text?.Trim() ?? "";
        if (query.Length < 2)
        {
            HideModelSuggestions();
            return;
        }

        RunDebouncedSuggestions(
            ref _modelDebounceCts,
            shouldSkip: () => _isSelectingModelSuggestion || ModelEntry.IsReadOnly,
            fetch: () =>
            {
                // TODO: [API] Replace with: var suggestions = await VehicleService.GetModelSuggestions(query)
                // Proposed endpoint: GET /vehicles/models/suggestions?q={query}
                return MockDataService.GetModelSuggestions(query);
            },
            show: ShowModelSuggestions);
    }

    // ── Autocomplete ─────────────────────────────────────

    private void AddItem(InvoiceItemRow item)
    {
        item.SubtotalChanged += (_, _) => RecalculateTotal();
        item.PropertyChanged += (s, args) =>
        {
            if (args.PropertyName == nameof(InvoiceItemRow.Description) && s is InvoiceItemRow row)
                OnItemDescriptionChanged(row);
        };
        _items.Add(item);
    }

    private void OnItemDescriptionChanged(InvoiceItemRow row)
    {
        if (_isSelectingSuggestion) return;

        _activeItemRow = row;
        var query = row.Description?.Trim() ?? "";
        var model = ModelEntry.Text?.Trim() ?? "";

        if (query.Length < 2 || string.IsNullOrWhiteSpace(model))
        {
            HideSuggestions();
            return;
        }

        RunDebouncedSuggestions(
            ref _debounceCts,
            shouldSkip: () => _isSelectingSuggestion,
            fetch: () =>
            {
                // TODO: [API] Replace with: var suggestions = await InvoiceItemService.GetSuggestions(model, query)
                // Maps to: GET /invoice-items/suggestions?model={model}&q={query}
                return MockDataService.GetItemSuggestions(model, query);
            },
            show: ShowSuggestions);
    }

    // ── Keyboard navigation ──────────────────────────────

    private void OnDescriptionEntryFocused(object? sender, FocusEventArgs e)
    {
        if (sender is not Entry entry) return;
        _activeDescriptionEntry = entry;
        _activeItemRow = entry.BindingContext as InvoiceItemRow;
        HookNativeKeyHandler(entry);
#if WINDOWS
        if (_activeItemRow is not null && entry.Handler?.PlatformView is Microsoft.UI.Xaml.Controls.TextBox textBox)
            _rowNativeTextBoxes[_activeItemRow] = textBox;
#endif
    }

    private void OnDescriptionEntryCompleted(object? sender, EventArgs e)
    {
        if (SuggestionsPopup.IsVisible && _currentSuggestions.Count > 0)
        {
            var index = _selectedSuggestionIndex >= 0 ? _selectedSuggestionIndex : 0;
            SelectSuggestionByKeyboard(index);
        }
        else
        {
            FocusPriceEntry();
        }
    }

    private void OnPriceEntryCompleted(object? sender, EventArgs e)
    {
        AddNewRowAndFocus();
    }

    private void FocusPriceEntry()
    {
        if (_activeDescriptionEntry?.Parent is not Border descBorder) return;
        if (descBorder.Parent is not Grid grid) return;

        foreach (var child in grid.Children)
        {
            if (child is Border border
                && Grid.GetColumn((BindableObject)border) == 2
                && border.Content is Entry priceEntry)
            {
                priceEntry.Focus();
                return;
            }
        }
    }

    private void AddNewRowAndFocus()
    {
        var newItem = new InvoiceItemRow();
        AddItem(newItem);
        _ = FocusDescriptionEntryForRow(newItem);
    }

    private async Task FocusDescriptionEntryForRow(InvoiceItemRow targetRow)
    {
        await Task.Delay(50); // Wait for BindableLayout to render
        foreach (var child in ItemsContainer.Children)
        {
            if (child is not VerticalStackLayout vsl) continue;
            foreach (var inner in vsl.Children)
            {
                if (inner is Border border && border.Content is Grid grid
                    && grid.BindingContext == targetRow)
                {
                    foreach (var gridChild in grid.Children)
                    {
                        if (gridChild is Border entryBorder
                            && Grid.GetColumn((BindableObject)entryBorder) == 1
                            && entryBorder.Content is Entry descEntry)
                        {
                            descEntry.Focus();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void SelectSuggestionByKeyboard(int index)
    {
        if (index < 0 || index >= _currentSuggestions.Count) return;
        SelectSuggestion(_currentSuggestions[index]);
        AddNewRowAndFocus();
    }

    private void HookNativeKeyHandler(Entry entry)
    {
#if WINDOWS
        if (entry.Handler?.PlatformView is Microsoft.UI.Xaml.Controls.TextBox textBox
            && _hookedNativeViews.Add(textBox))
        {
            textBox.PreviewKeyDown += OnNativeDescriptionKeyDown;
        }
#endif
    }

    private void DetachRowNativeKeyHandler(InvoiceItemRow row)
    {
#if WINDOWS
        if (_rowNativeTextBoxes.TryGetValue(row, out var nativeView))
        {
            if (nativeView is Microsoft.UI.Xaml.Controls.TextBox textBox)
                textBox.PreviewKeyDown -= OnNativeDescriptionKeyDown;
            _hookedNativeViews.Remove(nativeView);
            _rowNativeTextBoxes.Remove(row);
        }
#endif
    }

    private void DetachNativeKeyHandlers()
    {
#if WINDOWS
        foreach (var nativeView in _hookedNativeViews)
        {
            if (nativeView is Microsoft.UI.Xaml.Controls.TextBox textBox)
                textBox.PreviewKeyDown -= OnNativeDescriptionKeyDown;
        }
#endif
        _hookedNativeViews.Clear();
        _rowNativeTextBoxes.Clear();
    }

#if WINDOWS
    private void OnNativeDescriptionKeyDown(object sender, Microsoft.UI.Xaml.Input.KeyRoutedEventArgs e)
    {
        if (e.Key == Windows.System.VirtualKey.Down && ModelSuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            MoveModelSuggestionSelection(1);
        }
        else if (e.Key == Windows.System.VirtualKey.Up && ModelSuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            MoveModelSuggestionSelection(-1);
        }
        else if (e.Key == Windows.System.VirtualKey.Tab && ModelSuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            var index = _selectedModelSuggestionIndex >= 0 ? _selectedModelSuggestionIndex : 0;
            SelectModelSuggestionByKeyboard(index);
            FocusFirstDescriptionEntry();
        }
        else if (e.Key == Windows.System.VirtualKey.Enter && ModelSuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            var index = _selectedModelSuggestionIndex >= 0 ? _selectedModelSuggestionIndex : 0;
            SelectModelSuggestionByKeyboard(index);
        }
        else if (e.Key == Windows.System.VirtualKey.Down && SuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            MoveSuggestionSelection(1);
        }
        else if (e.Key == Windows.System.VirtualKey.Up && SuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            MoveSuggestionSelection(-1);
        }
        else if (e.Key == Windows.System.VirtualKey.Tab)
        {
            e.Handled = true;
            if (SuggestionsPopup.IsVisible && _currentSuggestions.Count > 0)
            {
                var index = _selectedSuggestionIndex >= 0 ? _selectedSuggestionIndex : 0;
                SelectSuggestionByKeyboard(index);
            }
            else
            {
                FocusPriceEntry();
            }
        }
        else if (e.Key == Windows.System.VirtualKey.Escape && SuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            HideSuggestions();
        }
        else if (e.Key == Windows.System.VirtualKey.Escape && ModelSuggestionsPopup.IsVisible)
        {
            e.Handled = true;
            HideModelSuggestions();
        }
    }
#endif

    private void MoveSuggestionSelection(int delta)
    {
        MoveSelection(_suggestionRows, ref _selectedSuggestionIndex, delta);
    }

    private void FocusFirstDescriptionEntry()
    {
        if (_items.Count == 0)
            return;

        _ = FocusDescriptionEntryForRow(_items[0]);
    }

    private void MoveModelSuggestionSelection(int delta)
    {
        MoveSelection(_modelSuggestionRows, ref _selectedModelSuggestionIndex, delta);
    }

    // ── Suggestions popup ────────────────────────────────

    private void ShowSuggestions(List<ItemSuggestion> suggestions)
    {
        HideModelSuggestions();

        ResetSuggestionState(SuggestionsContainer, _suggestionRows, _currentSuggestions, ref _selectedSuggestionIndex);

        if (suggestions.Count == 0 || _activeItemRow is null)
        {
            HideSuggestions();
            return;
        }

        _currentSuggestions.AddRange(suggestions);

        foreach (var suggestion in suggestions)
        {
            var row = new Grid
            {
                ColumnDefinitions =
                [
                    new ColumnDefinition(GridLength.Star),
                    new ColumnDefinition(GridLength.Auto),
                ],
                Padding = new Thickness(12, 8),
            };

            var descLabel = new Label
            {
                Text = suggestion.Description,
                FontFamily = "IBMPlexSans",
                FontSize = 13,
                TextColor = (Color)Application.Current!.Resources["TextPrimary"],
                VerticalTextAlignment = TextAlignment.Center,
            };
            Grid.SetColumn(descLabel, 0);

            var priceLabel = new Label
            {
                Text = MockDataService.FormatCurrency(suggestion.UnitPrice),
                FontFamily = "IBMPlexMono",
                FontSize = 12,
                TextColor = (Color)Application.Current!.Resources["TextMuted"],
                VerticalTextAlignment = TextAlignment.Center,
            };
            Grid.SetColumn(priceLabel, 1);

            row.Children.Add(descLabel);
            row.Children.Add(priceLabel);

            // Hover effect — also syncs keyboard selection index
            var capturedRow = row;
            var pointer = new PointerGestureRecognizer();
            pointer.PointerEntered += (_, _) =>
            {
                // Unhighlight keyboard selection if different
                if (_selectedSuggestionIndex >= 0 && _selectedSuggestionIndex < _suggestionRows.Count
                    && _suggestionRows[_selectedSuggestionIndex] != capturedRow)
                    _suggestionRows[_selectedSuggestionIndex].BackgroundColor = Colors.Transparent;

                capturedRow.BackgroundColor = Color.FromArgb("#F7F6F3");
                _selectedSuggestionIndex = _suggestionRows.IndexOf(capturedRow);
            };
            pointer.PointerExited += (_, _) => capturedRow.BackgroundColor = Colors.Transparent;
            row.GestureRecognizers.Add(pointer);

            // Tap to select (mouse click — no new row)
            var captured = suggestion;
            var tap = new TapGestureRecognizer();
            tap.Tapped += (_, _) => SelectSuggestion(captured);
            row.GestureRecognizers.Add(tap);

            _suggestionRows.Add(row);
            SuggestionsContainer.Children.Add(row);
            SuggestionsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current!.Resources["Border"],
            });
        }

        // Remove last divider
        if (SuggestionsContainer.Children.Count > 0)
            SuggestionsContainer.Children.RemoveAt(SuggestionsContainer.Children.Count - 1);

        PositionSuggestionsPopup();
        SuggestionsPopup.IsVisible = true;
        UpdateDismissOverlayVisibility();
    }

    private void HideSuggestions()
    {
        SuggestionsPopup.IsVisible = false;
        ResetSuggestionState(SuggestionsContainer, _suggestionRows, _currentSuggestions, ref _selectedSuggestionIndex);
        UpdateDismissOverlayVisibility();
    }

    private void ShowModelSuggestions(List<string> suggestions)
    {
        HideSuggestions();

        ResetSuggestionState(ModelSuggestionsContainer, _modelSuggestionRows, _currentModelSuggestions, ref _selectedModelSuggestionIndex);

        if (suggestions.Count == 0)
        {
            HideModelSuggestions();
            return;
        }

        _currentModelSuggestions.AddRange(suggestions);

        foreach (var suggestion in suggestions)
        {
            var row = new Grid
            {
                ColumnDefinitions = [new ColumnDefinition(GridLength.Star)],
                Padding = new Thickness(12, 8),
            };

            row.Children.Add(new Label
            {
                Text = suggestion,
                FontFamily = "IBMPlexSans",
                FontSize = 13,
                TextColor = (Color)Application.Current!.Resources["TextPrimary"],
                VerticalTextAlignment = TextAlignment.Center,
            });

            var capturedRow = row;
            var pointer = new PointerGestureRecognizer();
            pointer.PointerEntered += (_, _) =>
            {
                if (_selectedModelSuggestionIndex >= 0 && _selectedModelSuggestionIndex < _modelSuggestionRows.Count
                    && _modelSuggestionRows[_selectedModelSuggestionIndex] != capturedRow)
                    _modelSuggestionRows[_selectedModelSuggestionIndex].BackgroundColor = Colors.Transparent;

                capturedRow.BackgroundColor = Color.FromArgb("#F7F6F3");
                _selectedModelSuggestionIndex = _modelSuggestionRows.IndexOf(capturedRow);
            };
            pointer.PointerExited += (_, _) => capturedRow.BackgroundColor = Colors.Transparent;
            row.GestureRecognizers.Add(pointer);

            var capturedSuggestion = suggestion;
            var tap = new TapGestureRecognizer();
            tap.Tapped += (_, _) => SelectModelSuggestion(capturedSuggestion);
            row.GestureRecognizers.Add(tap);

            _modelSuggestionRows.Add(row);
            ModelSuggestionsContainer.Children.Add(row);
            ModelSuggestionsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current!.Resources["Border"],
            });
        }

        if (ModelSuggestionsContainer.Children.Count > 0)
            ModelSuggestionsContainer.Children.RemoveAt(ModelSuggestionsContainer.Children.Count - 1);

        PositionModelSuggestionsPopup();
        ModelSuggestionsPopup.IsVisible = true;
        UpdateDismissOverlayVisibility();
    }

    private void HideModelSuggestions()
    {
        ModelSuggestionsPopup.IsVisible = false;
        ResetSuggestionState(ModelSuggestionsContainer, _modelSuggestionRows, _currentModelSuggestions, ref _selectedModelSuggestionIndex);
        UpdateDismissOverlayVisibility();
    }

    private void SelectModelSuggestionByKeyboard(int index)
    {
        if (index < 0 || index >= _currentModelSuggestions.Count) return;
        SelectModelSuggestion(_currentModelSuggestions[index]);
    }

    private void SelectModelSuggestion(string suggestion)
    {
        CancelPendingModelAutocomplete();
        _isSelectingModelSuggestion = true;
        ModelEntry.Text = suggestion;
        _isSelectingModelSuggestion = false;
        HideModelSuggestions();
    }

    private void UpdateDismissOverlayVisibility()
    {
        DismissOverlay.IsVisible = SuggestionsPopup.IsVisible || ModelSuggestionsPopup.IsVisible;
    }

    private void SelectSuggestion(ItemSuggestion suggestion)
    {
        if (_activeItemRow is null) return;

        CancelPendingAutocomplete();
        _isSelectingSuggestion = true;
        _activeItemRow.Description = suggestion.Description;
        _activeItemRow.UnitPrice = $"{suggestion.UnitPrice:N0}".Replace(",", ".");
        _isSelectingSuggestion = false;
        HideSuggestions();
    }

    private void OnDismissOverlayTapped(object? sender, TappedEventArgs e)
    {
        HideSuggestions();
        HideModelSuggestions();
    }

    private void PositionSuggestionsPopup()
    {
        if (_activeDescriptionEntry is null) return;

#if WINDOWS
        // Use native WinUI TransformToVisual for pixel-accurate placement
        if (_activeDescriptionEntry.Handler?.PlatformView is Microsoft.UI.Xaml.UIElement entryNative
            && RootGrid.Handler?.PlatformView is Microsoft.UI.Xaml.UIElement rootNative)
        {
            var transform = entryNative.TransformToVisual(rootNative);
            var point = transform.TransformPoint(new Windows.Foundation.Point(0, 0));

            var entryHeight = entryNative.ActualSize.Y;
            var topOffset = point.Y + entryHeight + 2; // 2px gap below the entry
            var leftOffset = point.X;

            SuggestionsPopup.Margin = new Thickness(leftOffset, topOffset, 0, 0);
            SuggestionsPopup.HorizontalOptions = LayoutOptions.Start;
            SuggestionsPopup.VerticalOptions = LayoutOptions.Start;
            return;
        }
#endif

        // Fallback: position near items table
        SuggestionsPopup.Margin = new Thickness(78, 300, 0, 0);
        SuggestionsPopup.HorizontalOptions = LayoutOptions.Start;
        SuggestionsPopup.VerticalOptions = LayoutOptions.Start;
    }

    private void PositionModelSuggestionsPopup()
    {
#if WINDOWS
        if (ModelEntry.Handler?.PlatformView is Microsoft.UI.Xaml.UIElement modelNative
            && RootGrid.Handler?.PlatformView is Microsoft.UI.Xaml.UIElement rootNative)
        {
            var transform = modelNative.TransformToVisual(rootNative);
            var point = transform.TransformPoint(new Windows.Foundation.Point(0, 0));

            var entryHeight = modelNative.ActualSize.Y;
            var topOffset = point.Y + entryHeight + 2;
            var leftOffset = point.X;

            ModelSuggestionsPopup.Margin = new Thickness(leftOffset, topOffset, 0, 0);
            ModelSuggestionsPopup.HorizontalOptions = LayoutOptions.Start;
            ModelSuggestionsPopup.VerticalOptions = LayoutOptions.Start;
            return;
        }
#endif

        // Fallback under model field area.
        ModelSuggestionsPopup.Margin = new Thickness(24, 196, 0, 0);
        ModelSuggestionsPopup.HorizontalOptions = LayoutOptions.Start;
        ModelSuggestionsPopup.VerticalOptions = LayoutOptions.Start;
    }
}
