using ChepaMotos.Models;
using ChepaMotos.Services;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace ChepaMotos.ViewModels;

public class HomeViewModel : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;

    private string _kpiTotalValue = "$0";
    public string KpiTotalValue
    {
        get => _kpiTotalValue;
        private set => SetProperty(ref _kpiTotalValue, value);
    }

    private string _kpiTotalSubtitle = "0 facturas hoy";
    public string KpiTotalSubtitle
    {
        get => _kpiTotalSubtitle;
        private set => SetProperty(ref _kpiTotalSubtitle, value);
    }

    private string _kpiShopValue = "$0";
    public string KpiShopValue
    {
        get => _kpiShopValue;
        private set => SetProperty(ref _kpiShopValue, value);
    }

    private string _kpiAvgValue = "$0";
    public string KpiAvgValue
    {
        get => _kpiAvgValue;
        private set => SetProperty(ref _kpiAvgValue, value);
    }

    private string _kpiMechanicsValue = "0";
    public string KpiMechanicsValue
    {
        get => _kpiMechanicsValue;
        private set => SetProperty(ref _kpiMechanicsValue, value);
    }

    private string _kpiMechanicsSubtitle = "de 0 registrados";
    public string KpiMechanicsSubtitle
    {
        get => _kpiMechanicsSubtitle;
        private set => SetProperty(ref _kpiMechanicsSubtitle, value);
    }

    private bool _invoicesEmptyVisible = true;
    public bool InvoicesEmptyVisible
    {
        get => _invoicesEmptyVisible;
        private set => SetProperty(ref _invoicesEmptyVisible, value);
    }

    private bool _mechanicsEmptyVisible = true;
    public bool MechanicsEmptyVisible
    {
        get => _mechanicsEmptyVisible;
        private set => SetProperty(ref _mechanicsEmptyVisible, value);
    }

    public ObservableCollection<HomeInvoiceRowViewModel> InvoiceRows { get; } = [];
    public ObservableCollection<HomeMechanicRowViewModel> MechanicRows { get; } = [];

    public void LoadData()
    {
        LoadKpis();

        // TODO: [API] Replace with: var invoices = await InvoiceService.GetInvoices(date: DateTime.Today)
        // Maps to: GET /invoices?date=YYYY-MM-DD
        var todayInvoices = MockDataService.GetInvoices(date: DateTime.Today);

        // TODO: [API] Replace with: var activeMechanics = await MechanicService.GetMechanics(active: true)
        // Maps to: GET /mechanics?active=true
        var activeMechanics = MockDataService.GetMechanics(activeOnly: true);

        // TODO: [API] This is computed client-side from GET /invoices?date=today&type=SERVICE
        var invoiceCounts = MockDataService.GetTodayInvoiceCountByMechanic();

        InvoiceRows.Clear();
        foreach (var invoice in todayInvoices)
        {
            var isService = invoice.InvoiceType == "SERVICE";
            InvoiceRows.Add(new HomeInvoiceRowViewModel
            {
                SourceInvoice = invoice,
                IdText = $"{invoice.Id:D3}",
                TypeText = isService ? "Servicio" : "Venta",
                TypeBackgroundColor = isService ? Color.FromArgb("#F7E8E2") : Color.FromArgb("#DDE8F5"),
                TypeTextColor = isService ? Color.FromArgb("#C13B0A") : Color.FromArgb("#1A4A82"),
                ShowPlateBadge = isService && invoice.Vehicle is not null,
                PlateText = invoice.Vehicle?.Plate ?? "",
                BuyerText = invoice.BuyerName ?? "—",
                MechanicText = invoice.Mechanic?.Name?.Split(' ')[0] ?? "—",
                MechanicTextColor = invoice.Mechanic is null ? Color.FromArgb("#9A9790") : Color.FromArgb("#18170F"),
                TotalText = MockDataService.FormatCurrency(invoice.TotalAmount),
                TimeText = invoice.CreatedAt.ToString("HH:mm"),
                RowOpacity = invoice.IsCancelled ? 0.5 : 1.0,
            });
        }

        MechanicRows.Clear();
        foreach (var mechanic in activeMechanics)
        {
            var data = invoiceCounts.GetValueOrDefault(mechanic.Id, (0, 0m));
            MechanicRows.Add(new HomeMechanicRowViewModel
            {
                NameText = mechanic.Name.Split(' ')[0],
                CountText = $"{data.Item1} fact.",
            });
        }

        InvoicesEmptyVisible = InvoiceRows.Count == 0;
        MechanicsEmptyVisible = MechanicRows.Count == 0;
    }

    private void LoadKpis()
    {
        // TODO: [API] Replace with aggregated calls or a dedicated dashboard endpoint
        // KPI data is computed client-side from GET /invoices + GET /mechanics
        var (total, shopCut, avg, count, active, registered) = MockDataService.GetTodayKpis();

        KpiTotalValue = MockDataService.FormatCurrency(total);
        KpiTotalSubtitle = $"{count} facturas hoy";
        KpiShopValue = MockDataService.FormatCurrency(shopCut);
        KpiAvgValue = MockDataService.FormatCurrency(avg);
        KpiMechanicsValue = $"{active}";
        KpiMechanicsSubtitle = $"de {registered} registrados";
    }

    private void OnPropertyChanged([CallerMemberName] string? propertyName = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));

    private void SetProperty<T>(ref T backingField, T value, [CallerMemberName] string? propertyName = null)
    {
        if (EqualityComparer<T>.Default.Equals(backingField, value))
            return;

        backingField = value;
        OnPropertyChanged(propertyName);
    }
}

public class HomeInvoiceRowViewModel
{
    public required Invoice SourceInvoice { get; init; }
    public required string IdText { get; init; }
    public required string TypeText { get; init; }
    public required Color TypeBackgroundColor { get; init; }
    public required Color TypeTextColor { get; init; }
    public bool ShowPlateBadge { get; init; }
    public bool ShowBuyerText => !ShowPlateBadge;
    public required string PlateText { get; init; }
    public required string BuyerText { get; init; }
    public required string MechanicText { get; init; }
    public required Color MechanicTextColor { get; init; }
    public required string TotalText { get; init; }
    public required string TimeText { get; init; }
    public double RowOpacity { get; init; }
}

public class HomeMechanicRowViewModel
{
    public required string NameText { get; init; }
    public required string CountText { get; init; }
}