using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using ChepaMotos.Services;

namespace ChepaMotos.ViewModels;

public class MechanicsViewModel : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;

    private string _summaryText = "0 activos · 0 inactivos";
    public string SummaryText
    {
        get => _summaryText;
        private set => SetProperty(ref _summaryText, value);
    }

    private bool _emptyVisible;
    public bool EmptyVisible
    {
        get => _emptyVisible;
        private set => SetProperty(ref _emptyVisible, value);
    }

    public ObservableCollection<MechanicRowViewModel> Mechanics { get; } = [];

    public void LoadMechanics()
    {
        // TODO: [API] Replace with: var allMechanics = await MechanicService.GetMechanics(active: null)
        // Maps to: GET /mechanics (no active filter — returns all)
        var allMechanics = MockDataService.GetMechanics(activeOnly: null);
        var sorted = allMechanics
            .OrderByDescending(m => m.IsActive)
            .ThenBy(m => m.Name)
            .ToList();

        var activeCount = sorted.Count(m => m.IsActive);
        SummaryText = $"{activeCount} activos · {sorted.Count - activeCount} inactivos";

        // TODO: [API] This is computed client-side from GET /invoices?date=today&type=SERVICE
        var counts = MockDataService.GetTodayInvoiceCountByMechanic();

        Mechanics.Clear();
        foreach (var mech in sorted)
        {
            string subtitle;
            if (mech.IsActive && counts.TryGetValue(mech.Id, out var data))
            {
                subtitle = $"{data.count} factura{(data.count != 1 ? "s" : "")} hoy · {MockDataService.FormatCurrency(data.total)}";
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
                RowOpacity = isActive ? 1.0 : 0.6,
                BadgeText = isActive ? "Activo" : "Inactivo",
                BadgeBackgroundColor = isActive ? Color.FromArgb("#DFF0E8") : Color.FromArgb("#ECEAE4"),
                BadgeTextColor = isActive ? Color.FromArgb("#2A6E44") : Color.FromArgb("#9A9790"),
            });
        }

        EmptyVisible = Mechanics.Count == 0;
    }

    public void ToggleMechanicStatus(long mechanicId, bool isActive)
    {
        // TODO: [API] Replace with: await MechanicService.UpdateMechanicStatus(mechanicId, isActive)
        // Maps to: PATCH /mechanics/{id}/status  Body: { "is_active": bool }
        MockDataService.UpdateMechanicStatus(mechanicId, isActive);
        LoadMechanics();
    }

    public void AddMechanic(string name)
    {
        // TODO: [API] Replace with: await MechanicService.AddMechanic(name.Trim())
        // Maps to: POST /mechanics  Body: { "name": "..." }
        MockDataService.AddMechanic(name.Trim());
        LoadMechanics();
    }

    private void SetProperty<T>(ref T backingField, T value, [CallerMemberName] string? propertyName = null)
    {
        if (EqualityComparer<T>.Default.Equals(backingField, value))
            return;

        backingField = value;
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }
}

public class MechanicRowViewModel
{
    public long MechanicId { get; init; }
    public required string Name { get; init; }
    public required string Subtitle { get; init; }
    public bool IsActive { get; init; }
    public double RowOpacity { get; init; }
    public required string BadgeText { get; init; }
    public required Color BadgeBackgroundColor { get; init; }
    public required Color BadgeTextColor { get; init; }
}