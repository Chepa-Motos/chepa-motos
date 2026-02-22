namespace ChepaMotos.Views;

public partial class ServiceInvoicePage : ContentPage
{
    public ServiceInvoicePage()
    {
        InitializeComponent();
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
