using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using ChepaMotos.Models;
using ChepaMotos.Services;

namespace ChepaMotos.ViewModels;

public class InvoicesViewModel : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;

    private DateTime _selectedDate = DateTime.Today;
    public DateTime SelectedDate
    {
        get => _selectedDate;
        private set => SetProperty(ref _selectedDate, value);
    }

    private string _activeFilter = "Todas";
    public string ActiveFilter
    {
        get => _activeFilter;
        private set => SetProperty(ref _activeFilter, value);
    }

    private string _resultCountText = "0 resultados";
    public string ResultCountText
    {
        get => _resultCountText;
        private set => SetProperty(ref _resultCountText, value);
    }

    private bool _emptyVisible;
    public bool EmptyVisible
    {
        get => _emptyVisible;
        private set => SetProperty(ref _emptyVisible, value);
    }

    public ObservableCollection<InvoiceRowViewModel> Invoices { get; } = [];

    public void SetDate(DateTime date)
    {
        SelectedDate = date;
        LoadInvoices();
    }

    public void SetFilter(string filter)
    {
        ActiveFilter = filter;
        LoadInvoices();
    }

    public void LoadInvoices()
    {
        List<Invoice> invoices;
        if (ActiveFilter == "Anuladas")
        {
            // TODO: [API] Replace with: var invoices = await InvoiceService.GetInvoices(date, cancelled: true)
            // Maps to: GET /invoices?date=YYYY-MM-DD&cancelled=true
            invoices = MockDataService.GetInvoices(date: SelectedDate, cancelled: true);
        }
        else
        {
            string? type = ActiveFilter switch
            {
                "Servicio" => "SERVICE",
                "Venta" => "DELIVERY",
                _ => null,
            };
            // TODO: [API] Replace with: var invoices = await InvoiceService.GetInvoices(date, type)
            // Maps to: GET /invoices?date=YYYY-MM-DD&type=SERVICE|DELIVERY
            invoices = MockDataService.GetInvoices(date: SelectedDate, type: type, cancelled: false);
        }

        invoices = invoices.OrderByDescending(i => i.CreatedAt).ToList();

        ResultCountText = $"{invoices.Count} resultado{(invoices.Count != 1 ? "s" : "")}";
        EmptyVisible = invoices.Count == 0;

        Invoices.Clear();
        foreach (var invoice in invoices)
        {
            var isService = invoice.InvoiceType == "SERVICE";
            var isCancelled = invoice.IsCancelled;

            Invoices.Add(new InvoiceRowViewModel
            {
                SourceInvoice = invoice,
                IdText = invoice.Id.ToString("D3"),
                TypeText = isService ? "Servicio" : "Venta",
                TypeBackgroundColor = isService ? Color.FromArgb("#F7E8E2") : Color.FromArgb("#DDE8F5"),
                TypeTextColor = isService ? Color.FromArgb("#C13B0A") : Color.FromArgb("#1A4A82"),
                ShowPlateBadge = invoice.Vehicle is not null,
                PlateText = invoice.Vehicle?.Plate ?? "—",
                BuyerText = invoice.BuyerName ?? "—",
                BuyerTextColor = invoice.BuyerName is not null ? Color.FromArgb("#18170F") : Color.FromArgb("#9A9790"),
                MechanicText = invoice.Mechanic?.Name?.Split(' ')[0] ?? "—",
                MechanicTextColor = invoice.Mechanic is not null ? Color.FromArgb("#18170F") : Color.FromArgb("#9A9790"),
                TotalText = MockDataService.FormatCurrency(invoice.TotalAmount),
                StatusText = isCancelled ? "Anulada" : "Activa",
                StatusBackgroundColor = isCancelled ? Color.FromArgb("#FCE8E8") : Color.FromArgb("#DFF0E8"),
                StatusTextColor = isCancelled ? Color.FromArgb("#C0392B") : Color.FromArgb("#2A6E44"),
                TimeText = invoice.CreatedAt.ToString("HH:mm"),
                RowOpacity = isCancelled ? 0.5 : 1.0,
            });
        }
    }

    private void SetProperty<T>(ref T backingField, T value, [CallerMemberName] string? propertyName = null)
    {
        if (EqualityComparer<T>.Default.Equals(backingField, value))
            return;

        backingField = value;
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }
}

public class InvoiceRowViewModel
{
    public required Invoice SourceInvoice { get; init; }
    public required string IdText { get; init; }
    public required string TypeText { get; init; }
    public required Color TypeBackgroundColor { get; init; }
    public required Color TypeTextColor { get; init; }
    public bool ShowPlateBadge { get; init; }
    public bool ShowPlateDash => !ShowPlateBadge;
    public required string PlateText { get; init; }
    public required string BuyerText { get; init; }
    public required Color BuyerTextColor { get; init; }
    public required string MechanicText { get; init; }
    public required Color MechanicTextColor { get; init; }
    public required string TotalText { get; init; }
    public required string StatusText { get; init; }
    public required Color StatusBackgroundColor { get; init; }
    public required Color StatusTextColor { get; init; }
    public required string TimeText { get; init; }
    public double RowOpacity { get; init; }
}