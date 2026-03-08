using ChepaMotos.Models;
using ChepaMotos.Services;

namespace ChepaMotos.Views;

public partial class MechanicsView : ContentView
{
    public MechanicsView()
    {
        InitializeComponent();
        LoadMechanics();
    }

    private void LoadMechanics()
    {
        // TODO: [API] Replace with: var allMechanics = await MechanicService.GetMechanics(active: null)
        // Maps to: GET /mechanics (no active filter — returns all)
        var allMechanics = MockDataService.GetMechanics(activeOnly: null);
        var sorted = allMechanics
            .OrderByDescending(m => m.IsActive)
            .ThenBy(m => m.Name)
            .ToList();

        var activeCount = sorted.Count(m => m.IsActive);
        SummaryLabel.Text = $"{activeCount} activos · {sorted.Count - activeCount} inactivos";

        // TODO: [API] This is computed client-side from GET /invoices?date=today&type=SERVICE
        var counts = MockDataService.GetTodayInvoiceCountByMechanic();

        MechanicsContainer.Children.Clear();

        foreach (var mech in sorted)
        {
            var row = BuildMechanicRow(mech, counts);
            MechanicsContainer.Children.Add(row);
            MechanicsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current!.Resources["Border"]
            });
        }
    }

    private View BuildMechanicRow(Mechanic mech, Dictionary<long, (int count, decimal total)> counts)
    {
        var grid = new Grid
        {
            ColumnDefinitions =
            {
                new ColumnDefinition(GridLength.Star),
                new ColumnDefinition(GridLength.Auto),
                new ColumnDefinition(GridLength.Auto)
            },
            Padding = new Thickness(16, 14)
        };

        if (!mech.IsActive) grid.Opacity = 0.6;

        // Name + subtitle
        var info = new VerticalStackLayout { Spacing = 2 };

        info.Children.Add(new Label
        {
            Text = mech.Name,
            FontFamily = "IBMPlexSansMedium",
            FontSize = 14,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"]
        });

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

        info.Children.Add(new Label
        {
            Text = subtitle,
            FontFamily = "IBMPlexMono",
            FontSize = 11,
            TextColor = (Color)Application.Current!.Resources["TextMuted"]
        });

        Grid.SetColumn(info, 0);
        grid.Children.Add(info);

        // Status badge
        Color badgeBg, badgeText;
        string badgeLabel;
        if (mech.IsActive)
        {
            badgeBg = (Color)Application.Current!.Resources["GreenLight"];
            badgeText = (Color)Application.Current!.Resources["Green"];
            badgeLabel = "Activo";
        }
        else
        {
            badgeBg = (Color)Application.Current!.Resources["Surface2"];
            badgeText = (Color)Application.Current!.Resources["TextMuted"];
            badgeLabel = "Inactivo";
        }

        var badge = new Border
        {
            StrokeShape = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 4 },
            StrokeThickness = 0,
            BackgroundColor = badgeBg,
            Padding = new Thickness(8, 2),
            VerticalOptions = LayoutOptions.Center,
            Margin = new Thickness(0, 0, 12, 0),
            Content = new Label
            {
                Text = badgeLabel,
                FontFamily = "IBMPlexSansMedium",
                FontSize = 11,
                TextColor = badgeText
            }
        };
        Grid.SetColumn(badge, 1);
        grid.Children.Add(badge);

        // Toggle switch
        var toggle = new Switch
        {
            IsToggled = mech.IsActive,
            VerticalOptions = LayoutOptions.Center
        };
        toggle.Toggled += (s, e) => OnToggleMechanic(mech.Id, e.Value);
        Grid.SetColumn(toggle, 2);
        grid.Children.Add(toggle);

        return grid;
    }

    private void OnToggleMechanic(long mechanicId, bool isActive)
    {
        // TODO: [API] Replace with: await MechanicService.UpdateMechanicStatus(mechanicId, isActive)
        // Maps to: PATCH /mechanics/{id}/status  Body: { "is_active": bool }
        MockDataService.UpdateMechanicStatus(mechanicId, isActive);
        LoadMechanics();
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
            // TODO: [API] Replace with: await MechanicService.AddMechanic(name.Trim())
            // Maps to: POST /mechanics  Body: { "name": "..." }
            MockDataService.AddMechanic(name.Trim());
            LoadMechanics();
        }
    }
}

// Extension to find parent Page from a View
public static class ViewExtensions
{
    public static Page? FindParentPage(this Element element)
    {
        var current = element.Parent;
        while (current != null)
        {
            if (current is Page page) return page;
            current = current.Parent;
        }
        return null;
    }
}
