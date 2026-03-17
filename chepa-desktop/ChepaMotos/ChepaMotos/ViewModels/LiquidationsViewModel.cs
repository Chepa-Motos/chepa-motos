using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using ChepaMotos.Models;
using ChepaMotos.Services;

namespace ChepaMotos.ViewModels;

public class LiquidationsViewModel : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;

    private DateTime _selectedDate = DateTime.Today.AddDays(-1);
    public DateTime SelectedDate
    {
        get => _selectedDate;
        private set => SetProperty(ref _selectedDate, value);
    }

    private string _summaryTotalText = "$0";
    public string SummaryTotalText
    {
        get => _summaryTotalText;
        private set => SetProperty(ref _summaryTotalText, value);
    }

    private string _summaryMechanicText = "$0";
    public string SummaryMechanicText
    {
        get => _summaryMechanicText;
        private set => SetProperty(ref _summaryMechanicText, value);
    }

    private string _summaryShopText = "$0";
    public string SummaryShopText
    {
        get => _summaryShopText;
        private set => SetProperty(ref _summaryShopText, value);
    }

    private bool _emptyVisible;
    public bool EmptyVisible
    {
        get => _emptyVisible;
        private set => SetProperty(ref _emptyVisible, value);
    }

    public ObservableCollection<LiquidationRowViewModel> LiquidationRows { get; } = [];

    public void SetDate(DateTime date)
    {
        SelectedDate = date;
        LoadLiquidations();
    }

    public void LoadLiquidations()
    {
        // TODO: [API] Replace with: var liquidations = await LiquidationService.GetLiquidations(date: date)
        // Maps to: GET /liquidations?date=YYYY-MM-DD
        var liquidations = MockDataService.GetLiquidations(date: SelectedDate)
            .OrderBy(l => l.Mechanic?.Name)
            .ToList();

        SummaryTotalText = MockDataService.FormatCurrency(liquidations.Sum(l => l.TotalRevenue));
        SummaryMechanicText = MockDataService.FormatCurrency(liquidations.Sum(l => l.MechanicShare));
        SummaryShopText = MockDataService.FormatCurrency(liquidations.Sum(l => l.ShopShare));

        LiquidationRows.Clear();
        foreach (var liq in liquidations)
        {
            LiquidationRows.Add(new LiquidationRowViewModel
            {
                SourceLiquidation = liq,
                DateText = liq.Date,
                MechanicName = liq.Mechanic?.Name?.Split(' ')[0] ?? "—",
                InvoiceCountText = liq.InvoiceCount.ToString(),
                TotalRevenueText = MockDataService.FormatCurrency(liq.TotalRevenue),
                MechanicShareText = MockDataService.FormatCurrency(liq.MechanicShare),
                ShopShareText = MockDataService.FormatCurrency(liq.ShopShare),
            });
        }

        EmptyVisible = LiquidationRows.Count == 0;
    }

    private void SetProperty<T>(ref T backingField, T value, [CallerMemberName] string? propertyName = null)
    {
        if (EqualityComparer<T>.Default.Equals(backingField, value))
            return;

        backingField = value;
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
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