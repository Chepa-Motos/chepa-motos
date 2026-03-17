using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class LiquidationsView : ContentView
{
    private readonly LiquidationsViewModel _viewModel = new();

    public LiquidationsView()
    {
        InitializeComponent();
        BindingContext = _viewModel;
        // Default to yesterday since liquidations are end-of-day
        LiqDatePicker.Date = DateTime.Today.AddDays(-1);
        _viewModel.LoadLiquidations();
    }

    private void OnDateSelected(object? sender, DateChangedEventArgs e)
    {
        _viewModel.SetDate(e.NewDate ?? DateTime.Today.AddDays(-1));
    }
}
