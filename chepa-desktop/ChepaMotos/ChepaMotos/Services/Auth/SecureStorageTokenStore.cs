namespace ChepaMotos.Services.Auth;

public sealed class SecureStorageTokenStore : ITokenStore
{
    private const string AccessKey = "chepa.auth.access_token";
    private const string RefreshKey = "chepa.auth.refresh_token";

    public Task<string?> GetAccessTokenAsync()
        => SecureStorage.Default.GetAsync(AccessKey);

    public Task<string?> GetRefreshTokenAsync()
        => SecureStorage.Default.GetAsync(RefreshKey);

    public async Task SaveTokensAsync(string accessToken, string refreshToken)
    {
        await SecureStorage.Default.SetAsync(AccessKey, accessToken);
        await SecureStorage.Default.SetAsync(RefreshKey, refreshToken);
    }

    public Task ClearAsync()
    {
        SecureStorage.Default.Remove(AccessKey);
        SecureStorage.Default.Remove(RefreshKey);
        return Task.CompletedTask;
    }
}
