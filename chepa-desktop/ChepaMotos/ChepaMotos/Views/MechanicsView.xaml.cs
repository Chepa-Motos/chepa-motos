using ChepaMotos.Helpers;
using ChepaMotos.Services;
using ChepaMotos.ViewModels;

namespace ChepaMotos.Views;

public partial class MechanicsView : ContentView
{
    private readonly MechanicsViewModel _viewModel;
    private bool _hasMounted;

    public MechanicsView(MechanicsViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        BindingContext = _viewModel;

        _viewModel.MechanicAdded += OnMechanicAdded;
        _viewModel.MechanicStatusChanged += OnMechanicStatusChanged;
        _viewModel.OperationFailed += OnOperationFailed;
    }

    protected override void OnHandlerChanged()
    {
        base.OnHandlerChanged();
        if (Handler is not null && !_hasMounted)
        {
            _hasMounted = true;
            _ = _viewModel.ReloadAsync();
        }
        else if (Handler is null)
        {
            _viewModel.CancelOngoingOperation();
        }
    }

    private async void OnAddMechanicClicked(object? sender, EventArgs e)
    {
        var page = this.FindParentPage();
        if (page is null) return;

        if (!_viewModel.IsManager)
        {
            await page.DisplayAlertAsync(
                "Sin permisos",
                "Solo el rol GERENTE puede agregar mecánicos.",
                "Aceptar");
            return;
        }

        string? name = await page.DisplayPromptAsync(
            "Nuevo mecánico",
            "Ingresa el nombre completo del mecánico:",
            "Agregar",
            "Cancelar",
            placeholder: "Nombre completo",
            maxLength: 100);

        if (!string.IsNullOrWhiteSpace(name))
            await _viewModel.AddMechanicAsync(name.Trim());
    }

    private async void OnMechanicToggled(object? sender, ToggledEventArgs e)
    {
        if (sender is not Switch toggle || toggle.BindingContext is not MechanicRowViewModel row)
            return;

        // El binding ya cambió IsActive en la fila — solo emitimos el cambio si difiere
        // del valor que sabíamos (defensa contra reentrancia tras un Reload).
        if (row.IsActive == e.Value)
            return;

        await _viewModel.ToggleMechanicStatusAsync(row.MechanicId, e.Value);
    }

    private void OnMechanicAdded(object? sender, string name)
    {
        MainThread.BeginInvokeOnMainThread(() =>
            ToastService.ShowSuccess(this, $"Mecánico agregado: {name}"));
    }

    private void OnMechanicStatusChanged(object? sender, MechanicStatusChangedEventArgs e)
    {
        MainThread.BeginInvokeOnMainThread(() =>
            ToastService.ShowSuccess(this, e.IsActive ? "Mecánico activado" : "Mecánico desactivado"));
    }

    private void OnOperationFailed(object? sender, MechanicOperationFailedEventArgs e)
    {
        MainThread.BeginInvokeOnMainThread(async () =>
        {
            var page = this.FindParentPage();
            if (page is not null)
                await page.DisplayAlertAsync(e.Title, e.Message, "Aceptar");
        });
    }
}
