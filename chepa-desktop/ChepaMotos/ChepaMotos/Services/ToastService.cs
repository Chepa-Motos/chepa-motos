namespace ChepaMotos.Services;

using ChepaMotos.Helpers;

/// <summary>
/// Displays a dark toast notification at the bottom-right corner of a page.
/// Auto-dismisses after 3 seconds.
/// Design: #18170F background, white text, green indicator for success.
/// Injects the toast directly into the page's existing root layout — no re-parenting.
/// </summary>
public static class ToastService
{
    /// <summary>Show a success toast on the given page.</summary>
    public static void ShowSuccess(Page page, string message)
        => Show(page, message, ToastType.Success);

    /// <summary>Show an informational toast on the given page.</summary>
    public static void ShowInfo(Page page, string message)
        => Show(page, message, ToastType.Info);

    /// <summary>
    /// Show a toast overlaid on a ContentView's parent page.
    /// Convenience overload for views that are not pages.
    /// </summary>
    public static void ShowSuccess(ContentView view, string message)
    {
        var page = view.FindParentPage();
        if (page is not null)
            Show(page, message, ToastType.Success);
    }

    public static void ShowInfo(ContentView view, string message)
    {
        var page = view.FindParentPage();
        if (page is not null)
            Show(page, message, ToastType.Info);
    }

    private enum ToastType { Success, Info }

    private static void Show(Page page, string message, ToastType type)
    {
        // Find the existing root Grid — never create a wrapper or re-parent content
        var rootGrid = FindRootGrid(page);
        if (rootGrid is null) return;

        var indicatorColor = type == ToastType.Success
            ? Color.FromArgb("#2A6E44")     // Green
            : Color.FromArgb("#1A4A82");    // Blue

        var toast = BuildToast(message, indicatorColor);

        // Span all rows and columns so the toast can position at bottom-right
        var rowCount = rootGrid.RowDefinitions.Count;
        var colCount = rootGrid.ColumnDefinitions.Count;
        if (rowCount > 1) Grid.SetRowSpan(toast, rowCount);
        if (colCount > 1) Grid.SetColumnSpan(toast, colCount);

        rootGrid.Children.Add(toast);

        // Animate in
        toast.Opacity = 0;
        toast.TranslationY = 20;
        _ = toast.FadeToAsync(1, 200, Easing.CubicOut);
        _ = toast.TranslateToAsync(0, 0, 200, Easing.CubicOut);

        // Auto-dismiss after 3 seconds
        _ = DismissAfterDelay(rootGrid, toast, 3000);
    }

    private static async Task DismissAfterDelay(Grid rootGrid, View toast, int delayMs)
    {
        await Task.Delay(delayMs);
        await Task.WhenAll(
            toast.FadeToAsync(0, 250, Easing.CubicIn),
            toast.TranslateToAsync(0, 20, 250, Easing.CubicIn));
        rootGrid.Children.Remove(toast);
    }

    private static View BuildToast(string message, Color indicatorColor)
    {
        var indicator = new BoxView
        {
            WidthRequest = 4,
            HeightRequest = 20,
            CornerRadius = 2,
            Color = indicatorColor,
            VerticalOptions = LayoutOptions.Center,
        };

        var label = new Label
        {
            Text = message,
            FontFamily = "IBMPlexSansMedium",
            FontSize = 13,
            TextColor = Colors.White,
            VerticalTextAlignment = TextAlignment.Center,
        };

        var content = new HorizontalStackLayout
        {
            Spacing = 10,
            Children = { indicator, label },
        };

        var border = new Border
        {
            StrokeShape = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 8 },
            StrokeThickness = 0,
            BackgroundColor = Color.FromArgb("#18170F"),
            Padding = new Thickness(16, 12),
            HorizontalOptions = LayoutOptions.End,
            VerticalOptions = LayoutOptions.End,
            Margin = new Thickness(0, 0, 20, 20),
            InputTransparent = true,
            Shadow = new Shadow
            {
                Brush = new SolidColorBrush(Colors.Black),
                Offset = new Point(0, 4),
                Radius = 12,
                Opacity = 0.3f,
            },
            Content = content,
        };

        return border;
    }

    /// <summary>
    /// Finds the page's existing root Grid without modifying the visual tree.
    /// Returns null if the page content is not a Grid.
    /// </summary>
    private static Grid? FindRootGrid(Page page)
    {
        if (page is ContentPage contentPage && contentPage.Content is Grid grid)
            return grid;

        return null;
    }

}
