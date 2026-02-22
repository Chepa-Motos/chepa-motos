using Microsoft.Extensions.Logging;

namespace ChepaMotos
{
    public static class MauiProgram
    {
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

#if DEBUG
    		builder.Logging.AddDebug();
#endif

            return builder.Build();
        }
    }
}
