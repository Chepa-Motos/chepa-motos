using ChepaMotos.Helpers;
using ChepaMotos.Services;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class LiquidationsView : ContentView
{
    private readonly LiquidationsViewModel _viewModel;
    private bool _hasMounted;

    public LiquidationsView(LiquidationsViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        BindingContext = _viewModel;
        LiqDatePicker.Date = _viewModel.SelectedDate;

        _viewModel.LiquidationCompleted += OnLiquidationCompleted;
        _viewModel.LiquidationFailed += OnLiquidationFailed;
    }

    protected override void OnHandlerChanged()
    {
        base.OnHandlerChanged();
        if (Handler is not null && !_hasMounted)
        {
            _hasMounted = true;
            _ = _viewModel.ReloadAsync();
        }
    }

    private void OnDateSelected(object? sender, DateChangedEventArgs e)
    {
        _viewModel.SetDate(e.NewDate ?? DateTime.Today.AddDays(-1));
    }

    private void OnLiquidationCompleted(object? sender, LiquidationCompletedEventArgs e)
    {
        MainThread.BeginInvokeOnMainThread(() =>
        {
            var dateLabel = e.Date.ToString("dd/MM/yyyy");
            var msg = e.Count == 1
                ? $"Liquidación ejecutada para 1 mecánico ({dateLabel})"
                : $"Liquidación ejecutada para {e.Count} mecánicos ({dateLabel})";
            ToastService.ShowSuccess(this, msg);
        });
    }

    private void OnLiquidationFailed(object? sender, LiquidationFailedEventArgs e)
    {
        MainThread.BeginInvokeOnMainThread(async () =>
        {
            var page = this.FindParentPage();
            if (page is not null)
                await page.DisplayAlertAsync(e.Title, e.Message, "Aceptar");
        });
    }
}
