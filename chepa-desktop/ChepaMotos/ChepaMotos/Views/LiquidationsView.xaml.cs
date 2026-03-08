using ChepaMotos.Models;
using ChepaMotos.Services;

namespace ChepaMotos.Views;

public partial class LiquidationsView : ContentView
{
    public LiquidationsView()
    {
        InitializeComponent();
        // Default to yesterday since liquidations are end-of-day
        LiqDatePicker.Date = DateTime.Today.AddDays(-1);
        LoadLiquidations();
    }

    private void OnDateSelected(object? sender, DateChangedEventArgs e)
    {
        LoadLiquidations();
    }

    private void LoadLiquidations()
    {
        var date = LiqDatePicker.Date;
        // TODO: [API] Replace with: var liquidations = await LiquidationService.GetLiquidations(date: date)
        // Maps to: GET /liquidations?date=YYYY-MM-DD
        var liquidations = MockDataService.GetLiquidations(date: date)
            .OrderBy(l => l.Mechanic?.Name)
            .ToList();

        // Summary totals
        var totalRevenue = liquidations.Sum(l => l.TotalRevenue);
        var totalMechanic = liquidations.Sum(l => l.MechanicShare);
        var totalShop = liquidations.Sum(l => l.ShopShare);

        SummaryTotalLabel.Text = MockDataService.FormatCurrency(totalRevenue);
        SummaryMechanicLabel.Text = MockDataService.FormatCurrency(totalMechanic);
        SummaryShopLabel.Text = MockDataService.FormatCurrency(totalShop);

        EmptyLabel.IsVisible = liquidations.Count == 0;
        LiquidationRowsContainer.Children.Clear();

        foreach (var liq in liquidations)
        {
            var row = BuildRow(liq);
            LiquidationRowsContainer.Children.Add(row);
            LiquidationRowsContainer.Children.Add(new BoxView
            {
                HeightRequest = 1,
                Color = (Color)Application.Current!.Resources["Border"]
            });
        }
    }

    private View BuildRow(Liquidation liq)
    {
        var grid = new Grid
        {
            ColumnDefinitions =
            {
                new ColumnDefinition(new GridLength(120)),
                new ColumnDefinition(GridLength.Star),
                new ColumnDefinition(new GridLength(80)),
                new ColumnDefinition(new GridLength(120)),
                new ColumnDefinition(new GridLength(120)),
                new ColumnDefinition(new GridLength(120))
            },
            Padding = new Thickness(16, 10)
        };

        var dateLabel = new Label
        {
            Text = liq.Date,
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"],
            VerticalTextAlignment = TextAlignment.Center
        };
        Grid.SetColumn(dateLabel, 0);
        grid.Children.Add(dateLabel);

        var nameLabel = new Label
        {
            Text = liq.Mechanic?.Name?.Split(' ')[0] ?? "—",
            FontFamily = "IBMPlexSans",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"],
            VerticalTextAlignment = TextAlignment.Center
        };
        Grid.SetColumn(nameLabel, 1);
        grid.Children.Add(nameLabel);

        var countLabel = new Label
        {
            Text = liq.InvoiceCount.ToString(),
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"],
            VerticalTextAlignment = TextAlignment.Center,
            HorizontalTextAlignment = TextAlignment.Center
        };
        Grid.SetColumn(countLabel, 2);
        grid.Children.Add(countLabel);

        var totalLabel = new Label
        {
            Text = MockDataService.FormatCurrency(liq.TotalRevenue),
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"],
            VerticalTextAlignment = TextAlignment.Center,
            HorizontalTextAlignment = TextAlignment.End
        };
        Grid.SetColumn(totalLabel, 3);
        grid.Children.Add(totalLabel);

        var mechanicShareLabel = new Label
        {
            Text = MockDataService.FormatCurrency(liq.MechanicShare),
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["Green"],
            VerticalTextAlignment = TextAlignment.Center,
            HorizontalTextAlignment = TextAlignment.End
        };
        Grid.SetColumn(mechanicShareLabel, 4);
        grid.Children.Add(mechanicShareLabel);

        var shopShareLabel = new Label
        {
            Text = MockDataService.FormatCurrency(liq.ShopShare),
            FontFamily = "IBMPlexMono",
            FontSize = 13,
            TextColor = (Color)Application.Current!.Resources["TextPrimary"],
            VerticalTextAlignment = TextAlignment.Center,
            HorizontalTextAlignment = TextAlignment.End
        };
        Grid.SetColumn(shopShareLabel, 5);
        grid.Children.Add(shopShareLabel);

        return grid;
    }
}
