using ChepaMotos.Config;

namespace ChepaMotos.Views;

public partial class DashboardsView : ContentView
{
    private readonly IAppConfig _appConfig;

    public DashboardsView(IAppConfig appConfig)
    {
        _appConfig = appConfig;
        InitializeComponent();
        LoadMetabase();
    }

    private void LoadMetabase()
    {
        LoadingState.IsVisible = true;
        ErrorState.IsVisible = false;
        MetabaseWebView.Source = _appConfig.MetabaseUrl;
    }

    private void OnMetabaseNavigating(object? sender, WebNavigatingEventArgs e)
    {
        LoadingState.IsVisible = true;
        ErrorState.IsVisible = false;
    }

    private void OnMetabaseNavigated(object? sender, WebNavigatedEventArgs e)
    {
        LoadingState.IsVisible = false;

        if (e.Result == WebNavigationResult.Success)
        {
            ErrorState.IsVisible = false;
            return;
        }

        ErrorText.Text = "Verifica que Metabase este activo en el puerto 3000.";
        ErrorState.IsVisible = true;
    }

    private void OnRetryClicked(object? sender, EventArgs e)
    {
        LoadMetabase();
    }
}
