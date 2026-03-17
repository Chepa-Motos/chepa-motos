using ChepaMotos.Helpers;
using ChepaMotos.Services;
using ChepaMotos.ViewModels;
namespace ChepaMotos.Views;

public partial class MechanicsView : ContentView
{
    private readonly MechanicsViewModel _viewModel = new();

    public MechanicsView()
    {
        InitializeComponent();
        BindingContext = _viewModel;
        _viewModel.LoadMechanics();
    }

    private async void OnAddMechanicClicked(object? sender, EventArgs e)
    {
        // Find the parent page for DisplayPromptAsync
        var page = this.FindParentPage();
        if (page == null) return;

        string? name = await page.DisplayPromptAsync(
            "Nuevo mecánico",
            "Ingrese el nombre completo del mecánico:",
            "Agregar",
            "Cancelar",
            placeholder: "Nombre completo",
            maxLength: 100);

        if (!string.IsNullOrWhiteSpace(name))
        {
            _viewModel.AddMechanic(name.Trim());
            ToastService.ShowSuccess(this, "Mecánico agregado");
        }
    }

    private void OnMechanicToggled(object? sender, ToggledEventArgs e)
    {
        if (sender is not Switch toggle || toggle.BindingContext is not MechanicRowViewModel row)
            return;

        if (row.IsActive == e.Value)
            return;

        _viewModel.ToggleMechanicStatus(row.MechanicId, e.Value);
        ToastService.ShowSuccess(this, e.Value ? "Mecánico activado" : "Mecánico desactivado");
    }
}
