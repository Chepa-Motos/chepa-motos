using System.Collections.ObjectModel;
using ChepaMotos.Helpers;
using ChepaMotos.Models;
using ChepaMotos.Services.Domain;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ChepaMotos.ViewModels;

public partial class HomeViewModel : BaseViewModel
{
    private readonly IInvoiceService _invoiceService;
    private readonly IMechanicService _mechanicService;

    [ObservableProperty] private string _kpiTotalValue = "$0";
    [ObservableProperty] private string _kpiTotalSubtitle = "0 facturas hoy";
    [ObservableProperty] private string _kpiShopValue = "$0";
    [ObservableProperty] private string _kpiAvgValue = "$0";
    [ObservableProperty] private string _kpiMechanicsValue = "0";
    [ObservableProperty] private string _kpiMechanicsSubtitle = "de 0 registrados";

    [ObservableProperty] private bool _invoicesEmptyVisible = true;
    [ObservableProperty] private bool _mechanicsEmptyVisible = true;

    public ObservableCollection<HomeInvoiceRowViewModel> InvoiceRows { get; } = [];
    public ObservableCollection<HomeMechanicRowViewModel> MechanicRows { get; } = [];

    public HomeViewModel(IInvoiceService invoiceService, IMechanicService mechanicService)
    {
        _invoiceService = invoiceService;
        _mechanicService = mechanicService;
    }

    [RelayCommand]
    public Task ReloadAsync(CancellationToken ct = default) => ExecuteLoadAsync(async token =>
    {
        var today = DateTime.Today;
        var invoicesTask = _invoiceService.ListAsync(date: today, cancelled: false, ct: token);
        var activeMechanicsTask = _mechanicService.ListAsync(active: true, ct: token);
        var inactiveMechanicsTask = _mechanicService.ListAsync(active: false, ct: token);
        await Task.WhenAll(invoicesTask, activeMechanicsTask, inactiveMechanicsTask);

        var invoices = invoicesTask.Result;
        var activeMechanics = activeMechanicsTask.Result;
        var totalMechanicsCount = activeMechanics.Count + inactiveMechanicsTask.Result.Count;

        UpdateKpis(invoices, activeMechanics.Count, totalMechanicsCount);
        UpdateInvoicesList(invoices);
        UpdateMechanicsList(invoices, activeMechanics);
    }, ct);

    private void UpdateKpis(IReadOnlyList<Invoice> invoices, int activeMechanics, int totalMechanics)
    {
        // El backend ya filtra cancelled=false al pedir, pero aplicamos el filtro
        // local por si en el futuro la consulta cambia.
        var liveInvoices = invoices.Where(i => !i.IsCancelled).ToList();
        var total = liveInvoices.Sum(i => i.TotalAmount);
        var totalRevenue = liveInvoices.Sum(i => i.LaborAmount);
        var shopCut = totalRevenue * 0.30m;
        var avg = liveInvoices.Count > 0 ? total / liveInvoices.Count : 0m;

        KpiTotalValue = CurrencyFormatter.Format(total);
        KpiTotalSubtitle = $"{liveInvoices.Count} facturas hoy";
        KpiShopValue = CurrencyFormatter.Format(shopCut);
        KpiAvgValue = CurrencyFormatter.Format(avg);
        KpiMechanicsValue = activeMechanics.ToString();
        KpiMechanicsSubtitle = $"de {totalMechanics} registrados";
    }

    private void UpdateInvoicesList(IReadOnlyList<Invoice> invoices)
    {
        var ordered = invoices.OrderByDescending(i => i.CreatedAt).ToList();

        InvoiceRows.Clear();
        foreach (var invoice in ordered)
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
                TotalText = CurrencyFormatter.Format(invoice.TotalAmount),
                TimeText = invoice.CreatedAt.ToString("HH:mm"),
                RowOpacity = invoice.IsCancelled ? 0.5 : 1.0,
            });
        }

        InvoicesEmptyVisible = InvoiceRows.Count == 0;
    }

    private void UpdateMechanicsList(IReadOnlyList<Invoice> invoices, IReadOnlyList<Mechanic> activeMechanics)
    {
        var counts = invoices
            .Where(i => i.InvoiceType == "SERVICE" && !i.IsCancelled && i.Mechanic is not null)
            .GroupBy(i => i.Mechanic!.Id)
            .ToDictionary(g => g.Key, g => (Count: g.Count(), Total: g.Sum(i => i.TotalAmount)));

        MechanicRows.Clear();
        foreach (var mechanic in activeMechanics)
        {
            var count = counts.TryGetValue(mechanic.Id, out var data) ? data.Count : 0;
            MechanicRows.Add(new HomeMechanicRowViewModel
            {
                NameText = mechanic.Name.Split(' ')[0],
                CountText = $"{count} fact.",
            });
        }

        MechanicsEmptyVisible = MechanicRows.Count == 0;
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
