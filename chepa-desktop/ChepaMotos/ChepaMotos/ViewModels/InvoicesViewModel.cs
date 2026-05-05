using System.Collections.ObjectModel;
using ChepaMotos.Helpers;
using ChepaMotos.Models;
using ChepaMotos.Services.Domain;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ChepaMotos.ViewModels;

public partial class InvoicesViewModel : BaseViewModel
{
    private readonly IInvoiceService _invoiceService;
    private List<Invoice> _allInvoices = [];

    [ObservableProperty]
    private DateTime _selectedDate = DateTime.Today;

    [ObservableProperty]
    private string _activeFilter = "Todas";

    [ObservableProperty]
    private string _resultCountText = "0 resultados";

    /// <summary>
    /// Búsqueda client-side sobre la lista ya cargada del backend.
    /// Coincide en placa, comprador o descripción de cualquier ítem.
    /// Se resetea al cambiar fecha o filtro.
    /// </summary>
    [ObservableProperty]
    private string _searchQuery = string.Empty;

    public ObservableCollection<InvoiceRowViewModel> Invoices { get; } = [];

    public InvoicesViewModel(IInvoiceService invoiceService)
    {
        _invoiceService = invoiceService;
    }

    public void SetDate(DateTime date)
    {
        SelectedDate = date;
        SearchQuery = string.Empty;
        _ = ReloadAsync();
    }

    public void SetFilter(string filter)
    {
        ActiveFilter = filter;
        SearchQuery = string.Empty;
        _ = ReloadAsync();
    }

    partial void OnSearchQueryChanged(string value) => ApplyFilter();

    [RelayCommand]
    public Task ReloadAsync(CancellationToken ct = default) => ExecuteLoadAsync(async token =>
    {
        // Mapeo filtro UI → params del backend
        string? type;
        bool cancelled;
        switch (ActiveFilter)
        {
            case "Servicio":
                type = "SERVICE"; cancelled = false; break;
            case "Venta":
                type = "DELIVERY"; cancelled = false; break;
            case "Anuladas":
                type = null; cancelled = true; break;
            default: // "Todas"
                type = null; cancelled = false; break;
        }

        var invoices = await _invoiceService.ListAsync(
            date: SelectedDate,
            type: type,
            mechanicId: null,
            cancelled: cancelled,
            ct: token);

        UpdateRows(invoices);
    }, ct);

    private void UpdateRows(IReadOnlyList<Invoice> invoices)
    {
        _allInvoices = invoices.OrderByDescending(i => i.CreatedAt).ToList();
        ApplyFilter();
    }

    private void ApplyFilter()
    {
        var query = SearchQuery?.Trim() ?? string.Empty;
        var filtered = string.IsNullOrEmpty(query)
            ? _allInvoices
            : _allInvoices.Where(i => MatchesQuery(i, query)).ToList();

        ResultCountText = $"{filtered.Count} resultado{(filtered.Count != 1 ? "s" : "")}";
        IsCollectionEmpty = filtered.Count == 0;

        Invoices.Clear();
        foreach (var invoice in filtered)
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
                BikeText = invoice.Vehicle?.Model ?? "—",
                BikeTextColor = invoice.Vehicle is not null ? Color.FromArgb("#18170F") : Color.FromArgb("#9A9790"),
                BuyerText = invoice.BuyerName ?? "—",
                BuyerTextColor = invoice.BuyerName is not null ? Color.FromArgb("#18170F") : Color.FromArgb("#9A9790"),
                MechanicText = invoice.Mechanic?.Name?.Split(' ')[0] ?? "—",
                MechanicTextColor = invoice.Mechanic is not null ? Color.FromArgb("#18170F") : Color.FromArgb("#9A9790"),
                TotalText = CurrencyFormatter.Format(invoice.TotalAmount),
                StatusText = isCancelled ? "Anulada" : "Activa",
                StatusBackgroundColor = isCancelled ? Color.FromArgb("#FCE8E8") : Color.FromArgb("#DFF0E8"),
                StatusTextColor = isCancelled ? Color.FromArgb("#C0392B") : Color.FromArgb("#2A6E44"),
                TimeText = invoice.CreatedAt.ToString("HH:mm"),
                RowOpacity = isCancelled ? 0.5 : 1.0,
            });
        }
    }

    private static bool MatchesQuery(Invoice i, string query)
    {
        var cmp = StringComparison.OrdinalIgnoreCase;
        if (i.Vehicle?.Plate?.Contains(query, cmp) == true) return true;
        if (i.Vehicle?.Model?.Contains(query, cmp) == true) return true;
        if (i.BuyerName?.Contains(query, cmp) == true) return true;
        if (i.Mechanic?.Name?.Contains(query, cmp) == true) return true;
        return i.Items.Any(it => it.Description.Contains(query, cmp));
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
    public required string BikeText { get; init; }
    public required Color BikeTextColor { get; init; }
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
