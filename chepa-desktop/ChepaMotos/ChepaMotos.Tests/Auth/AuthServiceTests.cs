using System.Net;
using System.Text;
using System.Text.Json;
using ChepaMotos.Models;
using ChepaMotos.Services.Auth;
using ChepaMotos.Tests.TestHelpers;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using Xunit;

namespace ChepaMotos.Tests.Auth;

public class AuthServiceTests
{
    private readonly FakeHttpMessageHandler _handler = new();
    private readonly Mock<ITokenStore> _tokenStore = new();
    private readonly Mock<IAuthState> _authState = new();

    private AuthService CreateSut() => new(
        new FakeHttpClientFactory(_handler),
        _tokenStore.Object,
        _authState.Object,
        NullLogger<AuthService>.Instance);

    [Fact]
    public async Task LoginAsync_Success_PersistsTokensAndPublishesSession()
    {
        var jwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "admin",
            ["role"] = "GERENTE",
            ["exp"] = DateTimeOffset.UtcNow.AddMinutes(15).ToUnixTimeSeconds(),
        });
        _handler.EnqueueJson(HttpStatusCode.OK, $$"""
            {"data":{"tokenType":"Bearer","accessToken":"{{jwt}}","expiresIn":900,"refreshToken":"refresh-1"},"timestamp":"2026-04-29T10:00:00"}
            """);

        var sut = CreateSut();
        var tokens = await sut.LoginAsync("admin", "password");

        Assert.Equal(jwt, tokens.AccessToken);
        Assert.Equal("refresh-1", tokens.RefreshToken);
        _tokenStore.Verify(s => s.SaveTokensAsync(jwt, "refresh-1"), Times.Once);
        _authState.Verify(s => s.SetSession("admin", It.Is<IEnumerable<string>>(r => r.Contains("GERENTE"))), Times.Once);
    }

    [Fact]
    public async Task LoginAsync_InvalidCredentials_ThrowsAndDoesNotPersist()
    {
        _handler.EnqueueJson(HttpStatusCode.Unauthorized, """
            {"code":"INVALID_CREDENTIALS","message":"Bad password","timestamp":"2026-04-29T10:00:00"}
            """);

        var sut = CreateSut();

        var ex = await Assert.ThrowsAsync<ApiException>(() => sut.LoginAsync("admin", "wrong"));
        Assert.Equal(ApiErrorCodes.InvalidCredentials, ex.Code);

        _tokenStore.Verify(s => s.SaveTokensAsync(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
        _authState.Verify(s => s.SetSession(It.IsAny<string>(), It.IsAny<IEnumerable<string>>()), Times.Never);
    }

    [Fact]
    public async Task LogoutAsync_Success_ClearsLocalState()
    {
        _tokenStore.Setup(s => s.GetRefreshTokenAsync()).ReturnsAsync("refresh-1");
        _handler.EnqueueJson(HttpStatusCode.OK, """
            {"data":{"message":"Session closed"},"timestamp":"2026-04-29T10:00:00"}
            """);

        var sut = CreateSut();
        await sut.LogoutAsync();

        _tokenStore.Verify(s => s.ClearAsync(), Times.Once);
        _authState.Verify(s => s.RaiseLoggedOut(), Times.Once);
    }

    [Fact]
    public async Task LogoutAsync_ServerFails_StillClearsLocal()
    {
        _tokenStore.Setup(s => s.GetRefreshTokenAsync()).ReturnsAsync("refresh-1");
        _handler.EnqueueJson(HttpStatusCode.InternalServerError, """
            {"code":"INTERNAL_ERROR","message":"Boom","timestamp":"2026-04-29T10:00:00"}
            """);

        var sut = CreateSut();
        await sut.LogoutAsync();

        // Aunque el server falle, limpiamos local — el usuario quería cerrar sesión.
        _tokenStore.Verify(s => s.ClearAsync(), Times.Once);
        _authState.Verify(s => s.RaiseLoggedOut(), Times.Once);
    }

    [Fact]
    public async Task LogoutAsync_NoRefreshToken_SkipsServerCallButClearsLocal()
    {
        _tokenStore.Setup(s => s.GetRefreshTokenAsync()).ReturnsAsync((string?)null);

        var sut = CreateSut();
        await sut.LogoutAsync();

        Assert.Empty(_handler.Requests); // No se llamó al servidor.
        _tokenStore.Verify(s => s.ClearAsync(), Times.Once);
        _authState.Verify(s => s.RaiseLoggedOut(), Times.Once);
    }

    [Fact]
    public async Task GetValidAccessTokenAsync_NoSession_ReturnsNull()
    {
        _tokenStore.Setup(s => s.GetAccessTokenAsync()).ReturnsAsync((string?)null);

        var sut = CreateSut();
        var token = await sut.GetValidAccessTokenAsync();

        Assert.Null(token);
        Assert.Empty(_handler.Requests); // No intentó refresh.
    }

    [Fact]
    public async Task GetValidAccessTokenAsync_TokenStillValid_ReturnsCachedWithoutRefresh()
    {
        var jwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["exp"] = DateTimeOffset.UtcNow.AddMinutes(10).ToUnixTimeSeconds(),
        });
        _tokenStore.Setup(s => s.GetAccessTokenAsync()).ReturnsAsync(jwt);

        var sut = CreateSut();
        var token = await sut.GetValidAccessTokenAsync();

        Assert.Equal(jwt, token);
        Assert.Empty(_handler.Requests); // No refresh proactivo necesario.
    }

    [Fact]
    public async Task GetValidAccessTokenAsync_TokenExpiringSoon_TriggersProactiveRefresh()
    {
        // Token con < 60s de vida útil. El parser ve un exp casi expirado.
        var oldJwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["exp"] = DateTimeOffset.UtcNow.AddSeconds(10).ToUnixTimeSeconds(),
        });
        var newJwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["exp"] = DateTimeOffset.UtcNow.AddMinutes(15).ToUnixTimeSeconds(),
        });

        _tokenStore.Setup(s => s.GetAccessTokenAsync()).ReturnsAsync(oldJwt);
        _tokenStore.Setup(s => s.GetRefreshTokenAsync()).ReturnsAsync("refresh-1");

        // Respuesta del refresh endpoint.
        _handler.EnqueueJson(HttpStatusCode.OK, $$"""
            {"data":{"tokenType":"Bearer","accessToken":"{{newJwt}}","expiresIn":900,"refreshToken":"refresh-2"},"timestamp":"2026-04-29T10:00:00"}
            """);

        var sut = CreateSut();
        var token = await sut.GetValidAccessTokenAsync();

        Assert.Equal(newJwt, token);
        Assert.Single(_handler.Requests);
        Assert.EndsWith("auth/refresh", _handler.Requests[0].RequestUri!.AbsolutePath);
    }

    [Fact]
    public async Task GetValidAccessTokenAsync_RefreshFails_FallsBackToCachedToken()
    {
        var oldJwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["exp"] = DateTimeOffset.UtcNow.AddSeconds(10).ToUnixTimeSeconds(),
        });

        _tokenStore.Setup(s => s.GetAccessTokenAsync()).ReturnsAsync(oldJwt);
        _tokenStore.Setup(s => s.GetRefreshTokenAsync()).ReturnsAsync("refresh-1");

        // El refresh falla con red caída.
        _handler.EnqueueException(new HttpRequestException("connection refused"));

        var sut = CreateSut();
        var token = await sut.GetValidAccessTokenAsync();

        // Devuelve el token cacheado para que el ApiClient pueda intentar y caer al refresh reactivo.
        Assert.Equal(oldJwt, token);
    }

    private static string BuildJwt(IDictionary<string, object> payload)
    {
        const string header = "eyJhbGciOiJIUzI1NiJ9";
        var payloadJson = JsonSerializer.Serialize(payload);
        var payloadEncoded = Convert.ToBase64String(Encoding.UTF8.GetBytes(payloadJson))
            .Replace('+', '-').Replace('/', '_').TrimEnd('=');
        return $"{header}.{payloadEncoded}.fakesig";
    }
}
