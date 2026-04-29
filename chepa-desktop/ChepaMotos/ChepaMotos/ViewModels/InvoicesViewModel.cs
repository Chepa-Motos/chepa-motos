using System.Collections.ObjectModel;
using ChepaMotos.Models;
using ChepaMotos.Services.Domain;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ChepaMotos.ViewModels;

public partial class InvoicesViewModel : BaseViewModel
{
    private readonly IInvoiceService _invoiceService;

    [ObservableProperty]
    private DateTime _selectedDate = DateTime.Today;

    [ObservableProperty]
    private string _activeFilter = "Todas";

    [ObservableProperty]
    private string _resultCountText = "0 resultados";

    [ObservableProperty]
    private bool _emptyVisible;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsNotBusy))]
    [NotifyPropertyChangedFor(nameof(ShowSkeleton))]
    private bool _isBusy;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(HasLoadError))]
    [NotifyPropertyChangedFor(nameof(ShowSkeleton))]
    private string? _loadError;

    private bool _hasLoadedOnce;

    public bool IsNotBusy => !IsBusy;
    public bool HasLoadError => !string.IsNullOrEmpty(LoadError);
    public bool ShowSkeleton => IsBusy && !_hasLoadedOnce && !HasLoadError;

    public ObservableCollection<InvoiceRowViewModel> Invoices { get; } = [];

    public InvoicesViewModel(IInvoiceService invoiceService)
    {
        _invoiceService = invoiceService;
    }

    public void SetDate(DateTime date)
    {
        SelectedDate = date;
        _ = ReloadAsync();
    }

    public void SetFilter(string filter)
    {
        ActiveFilter = filter;
        _ = ReloadAsync();
    }

    [RelayCommand]
    public async Task ReloadAsync(CancellationToken ct = default)
    {
        if (IsBusy) return;
        var token = EnsureCancellationToken(ct);
        IsBusy = true;
        LoadError = null;

        try
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
            _hasLoadedOnce = true;
        }
        catch (OperationCanceledException) when (token.IsCancellationRequested)
        {
            return;
        }
        catch (ApiException ex)
        {
            LoadError = ex.Message;
        }
        catch (HttpRequestException)
        {
            LoadError = "No se pudo conectar al servidor. Verifica que esté encendido.";
        }
        catch (TaskCanceledException)
        {
            LoadError = "El servidor tardó demasiado en responder";
        }
        finally
        {
            IsBusy = false;
            OnPropertyChanged(nameof(ShowSkeleton));
        }
    }

    private void UpdateRows(IReadOnlyList<Invoice> invoices)
    {
        var ordered = invoices.OrderByDescending(i => i.CreatedAt).ToList();

        ResultCountText = $"{ordered.Count} resultado{(ordered.Count != 1 ? "s" : "")}";
        EmptyVisible = ordered.Count == 0;

        Invoices.Clear();
        foreach (var invoice in ordered)
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
                TotalText = FormatCurrency(invoice.TotalAmount),
                StatusText = isCancelled ? "Anulada" : "Activa",
                StatusBackgroundColor = isCancelled ? Color.FromArgb("#FCE8E8") : Color.FromArgb("#DFF0E8"),
                StatusTextColor = isCancelled ? Color.FromArgb("#C0392B") : Color.FromArgb("#2A6E44"),
                TimeText = invoice.CreatedAt.ToString("HH:mm"),
                RowOpacity = isCancelled ? 0.5 : 1.0,
            });
        }
    }

    private static string FormatCurrency(decimal value)
        => $"${value:N0}".Replace(",", ".");
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
