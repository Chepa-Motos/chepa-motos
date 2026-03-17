using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class InvoicesView : ContentView
{
    private readonly InvoicesViewModel _viewModel = new();

    public InvoicesView()
    {
        InitializeComponent();
        BindingContext = _viewModel;
        FilterDatePicker.Date = DateTime.Today;
        _viewModel.LoadInvoices();
        UpdateFilterVisuals();
    }

    private void OnDateSelected(object? sender, DateChangedEventArgs e)
    {
        _viewModel.SetDate(e.NewDate ?? DateTime.Today);
    }

    private void OnFilterTapped(object? sender, TappedEventArgs e)
    {
        if (e.Parameter is string filter)
        {
            _viewModel.SetFilter(filter);
            UpdateFilterVisuals();
        }
    }

    private void UpdateFilterVisuals()
    {
        var filters = new Dictionary<string, Border>
        {
            { "Todas", FilterAll },
            { "Servicio", FilterService },
            { "Venta", FilterDelivery },
            { "Anuladas", FilterCancelled }
        };

        foreach (var kvp in filters)
        {
            bool isActive = kvp.Key == _viewModel.ActiveFilter;
            kvp.Value.Stroke = isActive
                ? (Color)Application.Current!.Resources["BorderStrong"]
                : (Color)Application.Current!.Resources["Border"];

            var label = (Label)kvp.Value.Content!;
            label.TextColor = isActive
                ? (Color)Application.Current!.Resources["TextPrimary"]
                : (Color)Application.Current!.Resources["TextSecondary"];
        }
    }

    private void OnInvoiceRowTapped(object? sender, TappedEventArgs e)
    {
        if (sender is not Grid grid || grid.BindingContext is not InvoiceRowViewModel row)
            return;

        OpenInvoiceViewer(row.SourceInvoice);
    }

    private void OpenInvoiceViewer(ChepaMotos.Models.Invoice invoice)
    {
        var viewer = new InvoiceViewerPage(invoice);
        viewer.InvoiceCancelled += () => MainThread.BeginInvokeOnMainThread(_viewModel.LoadInvoices);
        var window = new Window(viewer)
        {
            Title = $"Factura #{invoice.Id:D3}",
            Width = 720,
            Height = 680
        };
        Application.Current?.OpenWindow(window);
    }
}
