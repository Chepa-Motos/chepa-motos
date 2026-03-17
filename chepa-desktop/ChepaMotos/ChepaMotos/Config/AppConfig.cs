namespace ChepaMotos.Config;

public static class AppConfig
{
    public static string BaseUrl { get; } =
        DeviceInfo.Platform == DevicePlatform.Android
            ? "http://10.0.2.2:8080/api"   // Android emulator → localhost
            : "http://localhost:8080/api";   // Windows dev

    public static string MetabaseUrl { get; } = "http://localhost:3000/embed/dashboard/";
}
