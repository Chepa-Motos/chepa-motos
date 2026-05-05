using System.Collections.ObjectModel;
using ChepaMotos.Helpers;
using ChepaMotos.Models;
using ChepaMotos.Services.Auth;
using ChepaMotos.Services.Domain;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace ChepaMotos.ViewModels;

public partial class MechanicsViewModel : BaseViewModel
{
    private readonly IMechanicService _mechanicService;
    private readonly IInvoiceService _invoiceService;

    private List<Mechanic> _allMechanics = [];
    private List<Invoice> _todayServiceInvoices = [];

    [ObservableProperty]
    private string _summaryText = "0 activos · 0 inactivos";

    /// <summary>Si está apagado (default), se ocultan los mecánicos inactivos.</summary>
    [ObservableProperty]
    private bool _showInactive;

    partial void OnShowInactiveChanged(bool value) => RebuildRows();

    /// <summary>True solo si el rol es GERENTE; controla la visibilidad del botón de
    /// crear y la habilitación de los switches por fila.</summary>
    public bool IsManager { get; }

    public ObservableCollection<MechanicRowViewModel> Mechanics { get; } = [];

    /// <summary>El code-behind muestra un toast cuando se añade un mecánico.</summary>
    public event EventHandler<string>? MechanicAdded;

    /// <summary>El code-behind muestra un toast cuando se cambia el estado.</summary>
    public event EventHandler<MechanicStatusChangedEventArgs>? MechanicStatusChanged;

    /// <summary>Operación falló — el code-behind muestra un alert.</summary>
    public event EventHandler<MechanicOperationFailedEventArgs>? OperationFailed;

    public MechanicsViewModel(
        IMechanicService mechanicService,
        IInvoiceService invoiceService,
        IAuthState authState)
    {
        _mechanicService = mechanicService;
        _invoiceService = invoiceService;
        IsManager = authState.IsManager;
    }

    [RelayCommand]
    public Task ReloadAsync(CancellationToken ct = default) => ExecuteLoadAsync(async token =>
    {
        // Mecánicos (activos + inactivos) y facturas SERVICE de hoy en paralelo
        // para calcular el subtítulo "X facturas hoy" por mecánico.
        var mechanicsTask = _mechanicService.ListAllAsync(token);
        var todayServiceTask = _invoiceService.ListAsync(
            date: DateTime.Today,
            type: "SERVICE",
            cancelled: false,
            ct: token);
        await Task.WhenAll(mechanicsTask, todayServiceTask);

        UpdateRows(mechanicsTask.Result, todayServiceTask.Result);
    }, ct);

    public async Task AddMechanicAsync(string name, CancellationToken ct = default)
    {
        if (!IsManager)
        {
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Sin permisos",
                "Solo el rol GERENTE puede agregar mecánicos."));
            return;
        }

        var trimmed = (name ?? string.Empty).Trim();
        if (string.IsNullOrEmpty(trimmed))
            return;

        var token = EnsureCancellationToken(ct);
        try
        {
            var created = await _mechanicService.CreateAsync(trimmed, token);
            await ReloadAsync(token);
            MechanicAdded?.Invoke(this, created.Name);
        }
        catch (OperationCanceledException) when (token.IsCancellationRequested)
        {
            return;
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.Forbidden)
        {
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Sin permisos",
                "Solo el rol GERENTE puede agregar mecánicos."));
        }
        catch (ApiException ex)
        {
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "No se pudo agregar el mecánico",
                ex.Message));
        }
        catch (HttpRequestException)
        {
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Sin conexión",
                "No se pudo conectar al servidor."));
        }
        catch (TaskCanceledException)
        {
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Tiempo agotado",
                "El servidor tardó demasiado en responder."));
        }
    }

    public async Task ToggleMechanicStatusAsync(long mechanicId, bool isActive, CancellationToken ct = default)
    {
        if (!IsManager)
        {
            // Re-sincronizar el switch a su estado anterior y avisar.
            await ReloadAsync(ct);
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Sin permisos",
                "Solo el rol GERENTE puede activar o desactivar mecánicos."));
            return;
        }

        var token = EnsureCancellationToken(ct);
        try
        {
            var updated = await _mechanicService.UpdateStatusAsync(mechanicId, isActive, token);
            await ReloadAsync(token);
            MechanicStatusChanged?.Invoke(this, new MechanicStatusChangedEventArgs(updated.Name, updated.IsActive));
        }
        catch (OperationCanceledException) when (token.IsCancellationRequested)
        {
            return;
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.Forbidden)
        {
            await ReloadAsync(token);
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Sin permisos",
                "Solo el rol GERENTE puede activar o desactivar mecánicos."));
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.MechanicNotFound)
        {
            await ReloadAsync(token);
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Mecánico no encontrado",
                "Este mecánico ya no existe en el servidor."));
        }
        catch (ApiException ex)
        {
            await ReloadAsync(token);
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "No se pudo actualizar el estado",
                ex.Message));
        }
        catch (HttpRequestException)
        {
            await ReloadAsync(token);
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Sin conexión",
                "No se pudo conectar al servidor."));
        }
        catch (TaskCanceledException)
        {
            await ReloadAsync(token);
            OperationFailed?.Invoke(this, new MechanicOperationFailedEventArgs(
                "Tiempo agotado",
                "El servidor tardó demasiado en responder."));
        }
    }

    private void UpdateRows(IReadOnlyList<Mechanic> allMechanics, IReadOnlyList<Invoice> todayServiceInvoices)
    {
        _allMechanics = allMechanics.ToList();
        _todayServiceInvoices = todayServiceInvoices.ToList();
        RebuildRows();
    }

    private void RebuildRows()
    {
        // El summary refleja el total real (no el filtrado), para que el usuario
        // siempre vea cuántos inactivos tiene aunque estén ocultos.
        var totalActive = _allMechanics.Count(m => m.IsActive);
        var totalInactive = _allMechanics.Count - totalActive;
        SummaryText = $"{totalActive} activos · {totalInactive} inactivos";

        var visible = _allMechanics
            .Where(m => ShowInactive || m.IsActive)
            .OrderByDescending(m => m.IsActive)
            .ThenBy(m => m.Name)
            .ToList();

        var counts = _todayServiceInvoices
            .Where(i => !i.IsCancelled && i.Mechanic is not null)
            .GroupBy(i => i.Mechanic!.Id)
            .ToDictionary(g => g.Key, g => (Count: g.Count(), Total: g.Sum(i => i.TotalAmount)));

        Mechanics.Clear();
        foreach (var mech in visible)
        {
            string subtitle;
            if (mech.IsActive && counts.TryGetValue(mech.Id, out var data))
            {
                subtitle = data.Count == 1
                    ? $"1 factura hoy · {CurrencyFormatter.Format(data.Total)}"
                    : $"{data.Count} facturas hoy · {CurrencyFormatter.Format(data.Total)}";
            }
            else if (mech.IsActive)
            {
                subtitle = "Sin facturas hoy";
            }
            else
            {
                subtitle = "Inactivo";
            }

            var isActive = mech.IsActive;
            Mechanics.Add(new MechanicRowViewModel
            {
                MechanicId = mech.Id,
                Name = mech.Name,
                Subtitle = subtitle,
                IsActive = isActive,
                CanToggle = IsManager,
                RowOpacity = isActive ? 1.0 : 0.6,
                BadgeText = isActive ? "Activo" : "Inactivo",
                BadgeBackgroundColor = isActive ? Color.FromArgb("#DFF0E8") : Color.FromArgb("#ECEAE4"),
                BadgeTextColor = isActive ? Color.FromArgb("#2A6E44") : Color.FromArgb("#9A9790"),
            });
        }

        IsCollectionEmpty = Mechanics.Count == 0;
    }
}

public class MechanicRowViewModel
{
    public long MechanicId { get; init; }
    public required string Name { get; init; }
    public required string Subtitle { get; init; }
    public bool IsActive { get; init; }
    /// <summary>El switch solo es interactivo si el usuario es GERENTE.</summary>
    public bool CanToggle { get; init; }
    public double RowOpacity { get; init; }
    public required string BadgeText { get; init; }
    public required Color BadgeBackgroundColor { get; init; }
    public required Color BadgeTextColor { get; init; }
}

public sealed record MechanicStatusChangedEventArgs(string Name, bool IsActive);
public sealed record MechanicOperationFailedEventArgs(string Title, string Message);
