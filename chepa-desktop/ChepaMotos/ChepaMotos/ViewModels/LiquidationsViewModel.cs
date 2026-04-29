using System.Collections.ObjectModel;
using ChepaMotos.Models;
using ChepaMotos.Services.Auth;
using ChepaMotos.Services.Domain;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ChepaMotos.ViewModels;

public partial class LiquidationsViewModel : BaseViewModel
{
    private readonly ILiquidationService _liquidationService;

    [ObservableProperty]
    private DateTime _selectedDate = DateTime.Today.AddDays(-1);

    [ObservableProperty]
    private string _summaryTotalText = "$0";

    [ObservableProperty]
    private string _summaryMechanicText = "$0";

    [ObservableProperty]
    private string _summaryShopText = "$0";

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

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(LiquidateButtonText))]
    private bool _isLiquidating;

    private bool _hasLoadedOnce;

    public bool IsNotBusy => !IsBusy;
    public bool HasLoadError => !string.IsNullOrEmpty(LoadError);
    public bool ShowSkeleton => IsBusy && !_hasLoadedOnce && !HasLoadError;

    /// <summary>True solo si el usuario tiene rol GERENTE; controla la visibilidad del botón "Liquidar día".</summary>
    public bool IsManager { get; }

    public string LiquidateButtonText => IsLiquidating ? "Liquidando…" : "Liquidar día";

    public ObservableCollection<LiquidationRowViewModel> LiquidationRows { get; } = [];

    /// <summary>
    /// Disparado cuando una liquidación se ejecuta con éxito. El code-behind
    /// muestra un toast con el resultado.
    /// </summary>
    public event EventHandler<LiquidationCompletedEventArgs>? LiquidationCompleted;

    /// <summary>
    /// Disparado cuando la liquidación falla. El code-behind muestra un alert
    /// con el mensaje. La UI puede seguir mostrando los datos previos.
    /// </summary>
    public event EventHandler<LiquidationFailedEventArgs>? LiquidationFailed;

    public LiquidationsViewModel(ILiquidationService liquidationService, IAuthState authState)
    {
        _liquidationService = liquidationService;
        IsManager = authState.IsManager;
    }

    public void SetDate(DateTime date)
    {
        SelectedDate = date;
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
            var liquidations = await _liquidationService.ListAsync(date: SelectedDate, ct: token);
            UpdateRows(liquidations);
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

    [RelayCommand]
    public async Task LiquidateDayAsync(CancellationToken ct = default)
    {
        if (IsLiquidating || !IsManager) return;

        var token = EnsureCancellationToken(ct);
        IsLiquidating = true;
        try
        {
            var liquidated = await _liquidationService.CreateAsync(
                date: SelectedDate,
                mechanicId: null,
                ct: token);

            // Refrescamos la lista para que aparezcan las nuevas liquidaciones.
            await ReloadAsync(token);

            LiquidationCompleted?.Invoke(this, new LiquidationCompletedEventArgs(liquidated.Count, SelectedDate));
        }
        catch (OperationCanceledException) when (token.IsCancellationRequested)
        {
            return;
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.LiquidationAlreadyExists)
        {
            // Algunos mecánicos ya estaban liquidados para esta fecha. Refrescamos
            // para mostrar el estado real y avisamos al usuario.
            await ReloadAsync(token);
            LiquidationFailed?.Invoke(this, new LiquidationFailedEventArgs(
                "Ya existe una liquidación",
                "Algún mecánico ya fue liquidado para esta fecha. Revisa la tabla."));
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.Forbidden)
        {
            LiquidationFailed?.Invoke(this, new LiquidationFailedEventArgs(
                "Sin permisos",
                "Solo el rol GERENTE puede ejecutar liquidaciones."));
        }
        catch (ApiException ex)
        {
            LiquidationFailed?.Invoke(this, new LiquidationFailedEventArgs("No se pudo liquidar", ex.Message));
        }
        catch (HttpRequestException)
        {
            LiquidationFailed?.Invoke(this, new LiquidationFailedEventArgs(
                "Sin conexión",
                "No se pudo conectar al servidor. Inténtalo de nuevo."));
        }
        catch (TaskCanceledException)
        {
            LiquidationFailed?.Invoke(this, new LiquidationFailedEventArgs(
                "Tiempo agotado",
                "El servidor tardó demasiado en responder."));
        }
        finally
        {
            IsLiquidating = false;
        }
    }

    private void UpdateRows(IReadOnlyList<Liquidation> liquidations)
    {
        var ordered = liquidations.OrderBy(l => l.Mechanic?.Name).ToList();

        SummaryTotalText = FormatCurrency(ordered.Sum(l => l.TotalRevenue));
        SummaryMechanicText = FormatCurrency(ordered.Sum(l => l.MechanicShare));
        SummaryShopText = FormatCurrency(ordered.Sum(l => l.ShopShare));

        LiquidationRows.Clear();
        foreach (var liq in ordered)
        {
            LiquidationRows.Add(new LiquidationRowViewModel
            {
                SourceLiquidation = liq,
                DateText = liq.Date,
                MechanicName = liq.Mechanic?.Name?.Split(' ')[0] ?? "—",
                InvoiceCountText = liq.InvoiceCount.ToString(),
                TotalRevenueText = FormatCurrency(liq.TotalRevenue),
                MechanicShareText = FormatCurrency(liq.MechanicShare),
                ShopShareText = FormatCurrency(liq.ShopShare),
            });
        }

        EmptyVisible = LiquidationRows.Count == 0;
    }

    private static string FormatCurrency(decimal value)
        => $"${value:N0}".Replace(",", ".");
}

public class LiquidationRowViewModel
{
    public required Liquidation SourceLiquidation { get; init; }
    public required string DateText { get; init; }
    public required string MechanicName { get; init; }
    public required string InvoiceCountText { get; init; }
    public required string TotalRevenueText { get; init; }
    public required string MechanicShareText { get; init; }
    public required string ShopShareText { get; init; }
}

public sealed record LiquidationCompletedEventArgs(int Count, DateTime Date);
public sealed record LiquidationFailedEventArgs(string Title, string Message);
