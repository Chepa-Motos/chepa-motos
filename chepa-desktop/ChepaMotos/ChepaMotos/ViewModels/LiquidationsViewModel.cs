using System.Collections.ObjectModel;
using ChepaMotos.Helpers;
using ChepaMotos.Models;
using ChepaMotos.Services.Auth;
using ChepaMotos.Services.Domain;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ChepaMotos.ViewModels;

public partial class LiquidationsViewModel : BaseViewModel
{
    private readonly ILiquidationService _liquidationService;
    private readonly IMechanicService _mechanicService;

    [ObservableProperty]
    private DateTime _selectedDate = DateTime.Today.AddDays(-1);

    /// <summary>
    /// Mecánico filtrado en el picker. Se popula una sola vez al montar la vista
    /// con "Todos los mecánicos" + la lista del backend. Cuando el usuario
    /// selecciona uno, se dispara <see cref="ReloadAsync"/> con su id.
    /// </summary>
    public ObservableCollection<MechanicOption> MechanicOptions { get; } = [];

    [ObservableProperty]
    private MechanicOption? _selectedMechanicOption;

    partial void OnSelectedMechanicOptionChanged(MechanicOption? value) => _ = ReloadAsync();

    [ObservableProperty]
    private string _summaryTotalText = "$0";

    [ObservableProperty]
    private string _summaryMechanicText = "$0";

    [ObservableProperty]
    private string _summaryShopText = "$0";

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(LiquidateButtonText))]
    private bool _isLiquidating;

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

    public LiquidationsViewModel(
        ILiquidationService liquidationService,
        IMechanicService mechanicService,
        IAuthState authState)
    {
        _liquidationService = liquidationService;
        _mechanicService = mechanicService;
        IsManager = authState.IsManager;

        // Sembrar la opción "Todos" para que el picker no quede vacío antes
        // de que LoadMechanicOptionsAsync agregue el resto.
        MechanicOptions.Add(MechanicOption.All);
        SelectedMechanicOption = MechanicOption.All;
    }

    /// <summary>
    /// Carga la lista de mecánicos para el picker. Idempotente — solo trae
    /// datos la primera vez. Si falla por red u otro error, el picker se queda
    /// con "Todos los mecánicos" y la pantalla sigue funcionando sin filtro.
    /// </summary>
    public async Task LoadMechanicOptionsIfNeededAsync(CancellationToken ct = default)
    {
        if (MechanicOptions.Count > 1) return;

        try
        {
            var all = await _mechanicService.ListAllAsync(ct);
            foreach (var m in all.OrderByDescending(m => m.IsActive).ThenBy(m => m.Name))
                MechanicOptions.Add(new MechanicOption(m.Id, m.Name));
        }
        catch
        {
            // Sin picker no se rompe nada — el filtro queda fijo en "Todos".
        }
    }

    public void SetDate(DateTime date)
    {
        SelectedDate = date;
        _ = ReloadAsync();
    }

    [RelayCommand]
    public Task ReloadAsync(CancellationToken ct = default) => ExecuteLoadAsync(async token =>
    {
        var liquidations = await _liquidationService.ListAsync(
            date: SelectedDate,
            mechanicId: SelectedMechanicOption?.Id,
            ct: token);
        UpdateRows(liquidations);
    }, ct);

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

        SummaryTotalText = CurrencyFormatter.Format(ordered.Sum(l => l.TotalRevenue));
        SummaryMechanicText = CurrencyFormatter.Format(ordered.Sum(l => l.MechanicShare));
        SummaryShopText = CurrencyFormatter.Format(ordered.Sum(l => l.ShopShare));

        LiquidationRows.Clear();
        foreach (var liq in ordered)
        {
            LiquidationRows.Add(new LiquidationRowViewModel
            {
                SourceLiquidation = liq,
                DateText = liq.Date,
                MechanicName = liq.Mechanic?.Name?.Split(' ')[0] ?? "—",
                InvoiceCountText = liq.InvoiceCount.ToString(),
                TotalRevenueText = CurrencyFormatter.Format(liq.TotalRevenue),
                MechanicShareText = CurrencyFormatter.Format(liq.MechanicShare),
                ShopShareText = CurrencyFormatter.Format(liq.ShopShare),
            });
        }

        IsCollectionEmpty = LiquidationRows.Count == 0;
    }

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

/// <summary>Opción del picker de mecánico — <c>Id == null</c> significa "Todos".</summary>
public sealed record MechanicOption(long? Id, string Name)
{
    public static readonly MechanicOption All = new(null, "Todos los mecánicos");
}
