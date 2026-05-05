using ChepaMotos.Services.Auth;
using ChepaMotos.Views;
using Microsoft.Extensions.DependencyInjection;

namespace ChepaMotos
{
    public partial class App : Application
    {
        private Window? _mainWindow;
        private bool _isClosing;
        private IServiceProvider? _services;
        private IAuthState? _authState;

        public App()
        {
            InitializeComponent();
        }

        protected override Window CreateWindow(IActivationState? activationState)
        {
            _services = IPlatformApplication.Current?.Services
                ?? throw new InvalidOperationException("MAUI service provider no disponible");
            _authState = _services.GetRequiredService<IAuthState>();
            _authState.SessionExpired += OnSessionExpired;
            _authState.LoggedOut += OnLoggedOut;

            var window = new Window
            {
                Title = "Chepa Motos",
                Width = 1280,
                Height = 800,
                MinimumWidth = 1024,
                MinimumHeight = 600,
                Page = CreateLoginPage(),
            };
            _mainWindow = window;

#if WINDOWS
            var closeHooked = false;
            window.HandlerChanged += (s, e) =>
            {
                if (closeHooked)
                    return;

                if (window.Handler?.PlatformView is Microsoft.UI.Xaml.Window nativeWindow)
                {
                    var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(nativeWindow);
                    var windowId = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(hwnd);
                    var appWindow = Microsoft.UI.Windowing.AppWindow.GetFromWindowId(windowId);
                    closeHooked = true;

                    appWindow.Closing += async (appWin, args) =>
                    {
                        if (_isClosing) return;
                        args.Cancel = true;
                        var mainPage = window.Page;
                        if (mainPage == null)
                        {
                            // If the main page is unexpectedly null, do not block closing the window.
                            args.Cancel = false;
                            return;
                        }

                        bool confirm = await mainPage.DisplayAlertAsync(
                            "Cerrar aplicación",
                            "¿Estás seguro de que deseas cerrar Chepa Motos? Se cerrarán todas las ventanas abiertas.",
                            "Sí, cerrar",
                            "Volver");

                        if (confirm)
                        {
                            _isClosing = true;
                            var otherWindows = Windows.Where(w => w != _mainWindow).ToList();
                            foreach (var w in otherWindows)
                                CloseWindow(w);

                            Current?.Quit();
                        }
                    };

                    if (appWindow.Presenter is Microsoft.UI.Windowing.OverlappedPresenter presenter)
                    {
                        presenter.Maximize();
                    }
                }
            };
#endif

            return window;
        }

        private LoginPage CreateLoginPage()
        {
            var page = _services!.GetRequiredService<LoginPage>();
            page.LoginSucceeded += OnSessionAcquired;
            page.ContinueRequested += OnSessionAcquired;
            return page;
        }

        private void OnSessionAcquired(object? sender, EventArgs e)
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                if (_mainWindow is null || _services is null) return;
                _mainWindow.Page = _services.GetRequiredService<MainLayout>();
            });
        }

        private void OnSessionExpired(object? sender, EventArgs e)
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                if (_mainWindow is null) return;
                var login = CreateLoginPage();
                login.ShowSessionExpiredMessage();
                _mainWindow.Page = login;
            });
        }

        private void OnLoggedOut(object? sender, EventArgs e)
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                if (_mainWindow is null) return;
                _mainWindow.Page = CreateLoginPage();
            });
        }
    }
}
