using ChepaMotos.Views;

namespace ChepaMotos
{
    public partial class App : Application
    {
        private Window? _mainWindow;

        public App()
        {
            InitializeComponent();
        }

        protected override Window CreateWindow(IActivationState? activationState)
        {
            var window = new Window(new MainLayout());
            window.Title = "Chepa Motos";
            window.Width = 1280;
            window.Height = 800;
            window.MinimumWidth = 1024;
            window.MinimumHeight = 600;
            _mainWindow = window;

#if WINDOWS
            window.HandlerChanged += (s, e) =>
            {
                if (window.Handler?.PlatformView is Microsoft.UI.Xaml.Window nativeWindow)
                {
                    var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(nativeWindow);
                    var windowId = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(hwnd);
                    var appWindow = Microsoft.UI.Windowing.AppWindow.GetFromWindowId(windowId);

                    appWindow.Closing += async (appWin, args) =>
                    {
                        args.Cancel = true;
                        var mainPage = window.Page;
                        if (mainPage == null) return;

                        bool confirm = await mainPage.DisplayAlertAsync(
                            "Cerrar aplicación",
                            "¿Estás seguro de que deseas cerrar Chepa Motos? Se cerrarán todas las ventanas abiertas.",
                            "Sí, cerrar",
                            "Volver");

                        if (confirm)
                        {
                            // Close all invoice windows first
                            var otherWindows = Windows.Where(w => w != _mainWindow).ToList();
                            foreach (var w in otherWindows)
                                CloseWindow(w);

                            // Now close the main window for real
                            appWin.Closing -= null; // prevent re-entry
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
    }
}