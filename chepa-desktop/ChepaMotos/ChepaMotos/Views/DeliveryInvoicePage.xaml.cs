namespace ChepaMotos.Views;

public partial class DeliveryInvoicePage : ContentPage
{
    public DeliveryInvoicePage()
    {
        InitializeComponent();
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
}
