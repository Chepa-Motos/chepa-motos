using ChepaMotos.Config;
using ChepaMotos.Models;
using ChepaMotos.Services;
using ChepaMotos.Services.Api;
using ChepaMotos.Services.Auth;
using ChepaMotos.Services.Domain;
using ChepaMotos.Services.Logging;
using ChepaMotos.ViewModels;
using ChepaMotos.Views;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace ChepaMotos
{
    public static class MauiProgram
    {
        /// <summary>
        /// Compat alias — la constante real vive en
        /// <see cref="Services.Api.ApiClientNames.ChepaApi"/>.
        /// </summary>
        public const string ApiHttpClientName = Services.Api.ApiClientNames.ChepaApi;

        public static MauiApp CreateMauiApp()
        {
            var builder = MauiApp.CreateBuilder();
            builder
                .UseMauiApp<App>()
                .ConfigureFonts(fonts =>
                {
                    // IBM Plex Sans
                    fonts.AddFont("IBMPlexSans-Light.ttf", "IBMPlexSansLight");
                    fonts.AddFont("IBMPlexSans-Regular.ttf", "IBMPlexSans");
                    fonts.AddFont("IBMPlexSans-Medium.ttf", "IBMPlexSansMedium");
                    fonts.AddFont("IBMPlexSans-SemiBold.ttf", "IBMPlexSansSemiBold");
                    fonts.AddFont("IBMPlexSans-Bold.ttf", "IBMPlexSansBold");

                    // IBM Plex Mono
                    fonts.AddFont("IBMPlexMono-Regular.ttf", "IBMPlexMono");
                    fonts.AddFont("IBMPlexMono-Medium.ttf", "IBMPlexMonoMedium");
                    fonts.AddFont("IBMPlexMono-SemiBold.ttf", "IBMPlexMonoSemiBold");
                });

            // Configuración leída desde Resources/Raw/appsettings.json con override
            // opcional desde %LOCALAPPDATA%/ChepaMotos/appsettings.json. Resolvemos
            // antes del AddHttpClient para que la BaseUrl/Timeout vengan del JSON.
            var appConfig = Config.AppConfig.Load();
            builder.Services.AddSingleton<IAppConfig>(appConfig);

            builder.Services.AddHttpClient(ApiHttpClientName, client =>
            {
                client.BaseAddress = new Uri(appConfig.BaseUrl.TrimEnd('/') + "/");
                client.Timeout = TimeSpan.FromSeconds(appConfig.TimeoutSeconds);
            });

            // Auth + ApiClient (singletons: estado de sesión y candado de refresh deben ser compartidos)
            builder.Services.AddSingleton<ITokenStore, SecureStorageTokenStore>();
            builder.Services.AddSingleton<IAuthState, AuthState>();
            builder.Services.AddSingleton<IAuthService, AuthService>();
            builder.Services.AddSingleton<IApiClient, ApiClient>();

            // Servicios por dominio (transient: stateless, una instancia por uso)
            builder.Services.AddTransient<IMechanicService, MechanicService>();
            builder.Services.AddTransient<IVehicleService, VehicleService>();
            builder.Services.AddTransient<IInvoiceItemService, InvoiceItemService>();
            builder.Services.AddTransient<IInvoiceService, InvoiceService>();
            builder.Services.AddTransient<ILiquidationService, LiquidationService>();

            // PDF service (singleton: la inicialización del licence de QuestPDF es one-shot).
            builder.Services.AddSingleton<IInvoicePdfService, InvoicePdfService>();

            // ViewModels y Pages (transient: una instancia nueva por cada navegación)
            builder.Services.AddTransient<LoginViewModel>();
            builder.Services.AddTransient<HomeViewModel>();
            builder.Services.AddTransient<InvoicesViewModel>();
            builder.Services.AddTransient<LiquidationsViewModel>();
            builder.Services.AddTransient<MechanicsViewModel>();

            builder.Services.AddTransient<LoginPage>();
            builder.Services.AddTransient<MainLayout>();
            builder.Services.AddTransient<HomeView>();
            builder.Services.AddTransient<InvoicesView>();
            builder.Services.AddTransient<LiquidationsView>();
            builder.Services.AddTransient<DashboardsView>();
            builder.Services.AddTransient<MechanicsView>();
            builder.Services.AddTransient<ServiceInvoicePage>();
            builder.Services.AddTransient<DeliveryInvoicePage>();

            // El visor recibe la Invoice como parámetro de construcción + servicios via DI.
            // Registramos un delegado factory para que cualquier view pueda crearlo.
            builder.Services.AddTransient<Func<Invoice, InvoiceViewerPage>>(sp =>
                invoice => ActivatorUtilities.CreateInstance<InvoiceViewerPage>(sp, invoice));

            // Logging a archivo plano: %LOCALAPPDATA%\ChepaMotos\logs\chepa-{yyyyMMdd}.log
            // — diagnóstico en producción sin abrir Visual Studio.
            var logsPath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "ChepaMotos", "logs");
            builder.Logging.AddProvider(new FileLoggerProvider(logsPath));
            builder.Logging.SetMinimumLevel(LogLevel.Information);

#if DEBUG
    		builder.Logging.AddDebug();
#endif

            return builder.Build();
        }
    }
}
