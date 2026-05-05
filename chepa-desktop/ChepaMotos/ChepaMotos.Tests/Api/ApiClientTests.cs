using System.Net;
using ChepaMotos.Models;
using ChepaMotos.Services.Api;
using ChepaMotos.Services.Auth;
using ChepaMotos.Tests.TestHelpers;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using Xunit;

namespace ChepaMotos.Tests.Api;

public class ApiClientTests
{
    private readonly FakeHttpMessageHandler _handler = new();
    private readonly Mock<ITokenStore> _tokenStore = new();
    private readonly Mock<IAuthService> _authService = new();
    private readonly Mock<IAuthState> _authState = new();

    private ApiClient CreateSut() => new(
        new FakeHttpClientFactory(_handler),
        _tokenStore.Object,
        _authService.Object,
        _authState.Object,
        NullLogger<ApiClient>.Instance);

    [Fact]
    public async Task GetAsync_Success_DeserializesEnvelope()
    {
        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync("token");

        _handler.EnqueueJson(HttpStatusCode.OK,
            """{"data":{"id":1,"name":"Pedro","is_active":true},"timestamp":"2026-04-29T10:00:00"}""");

        var sut = CreateSut();
        var mechanic = await sut.GetAsync<Mechanic>("mechanics/1");

        Assert.Equal(1, mechanic.Id);
        Assert.Equal("Pedro", mechanic.Name);
        Assert.True(mechanic.IsActive);
    }

    [Fact]
    public async Task GetAsync_4xxError_ThrowsApiExceptionWithCode()
    {
        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync((string?)null);

        _handler.EnqueueJson(HttpStatusCode.NotFound,
            """{"code":"MECHANIC_NOT_FOUND","message":"Mechanic not found","timestamp":"2026-04-29T10:00:00"}""");

        var sut = CreateSut();

        var ex = await Assert.ThrowsAsync<ApiException>(() => sut.GetAsync<Mechanic>("mechanics/999"));
        Assert.Equal("MECHANIC_NOT_FOUND", ex.Code);
        Assert.Equal(404, ex.StatusCode);
    }

    [Fact]
    public async Task GetAsync_401AuthRequired_RefreshesAndRetries()
    {
        // Primer intento: 401 AUTH_REQUIRED.
        _handler.EnqueueJson(HttpStatusCode.Unauthorized,
            """{"code":"AUTH_REQUIRED","message":"Authentication is required","timestamp":"2026-04-29T10:00:00"}""");
        // Segundo intento (tras refresh): 200 OK.
        _handler.EnqueueJson(HttpStatusCode.OK,
            """{"data":{"id":1,"name":"Pedro","is_active":true},"timestamp":"2026-04-29T10:00:00"}""");

        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync("any-token");
        _tokenStore.Setup(s => s.GetRefreshTokenAsync())
                   .ReturnsAsync("refresh-token");
        _authService.Setup(s => s.RefreshAsync("refresh-token", It.IsAny<CancellationToken>()))
                    .ReturnsAsync(new AuthTokens { AccessToken = "new-access", RefreshToken = "new-refresh" });

        var sut = CreateSut();
        var mechanic = await sut.GetAsync<Mechanic>("mechanics/1");

        Assert.Equal(1, mechanic.Id);
        _authService.Verify(s => s.RefreshAsync("refresh-token", It.IsAny<CancellationToken>()), Times.Once);
        Assert.Equal(2, _handler.Requests.Count);
    }

    [Fact]
    public async Task GetAsync_401_RefreshFails_RaisesSessionExpired()
    {
        _handler.EnqueueJson(HttpStatusCode.Unauthorized,
            """{"code":"AUTH_REQUIRED","message":"Authentication is required","timestamp":"2026-04-29T10:00:00"}""");

        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync("any-token");
        _tokenStore.Setup(s => s.GetRefreshTokenAsync())
                   .ReturnsAsync("refresh-token");
        _authService.Setup(s => s.RefreshAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()))
                    .ThrowsAsync(new ApiException(ApiErrorCodes.SessionExpired, "Refresh expired", 401));

        var sut = CreateSut();

        await Assert.ThrowsAsync<ApiException>(() => sut.GetAsync<Mechanic>("mechanics/1"));
        _authState.Verify(s => s.RaiseSessionExpired(), Times.Once);
    }

    [Fact]
    public async Task GetAsync_401_NoRefreshToken_RaisesSessionExpired()
    {
        _handler.EnqueueJson(HttpStatusCode.Unauthorized,
            """{"code":"AUTH_REQUIRED","message":"Authentication is required","timestamp":"2026-04-29T10:00:00"}""");

        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync("any-token");
        _tokenStore.Setup(s => s.GetRefreshTokenAsync())
                   .ReturnsAsync((string?)null);

        var sut = CreateSut();

        await Assert.ThrowsAsync<ApiException>(() => sut.GetAsync<Mechanic>("mechanics/1"));
        _authState.Verify(s => s.RaiseSessionExpired(), Times.Once);
        _authService.Verify(s => s.RefreshAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task GetAsync_HttpRequestException_RetriesOnce()
    {
        _handler.EnqueueException(new HttpRequestException("connection refused"));
        _handler.EnqueueJson(HttpStatusCode.OK,
            """{"data":{"id":1,"name":"Pedro","is_active":true},"timestamp":"2026-04-29T10:00:00"}""");

        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync((string?)null);

        var sut = CreateSut();
        var mechanic = await sut.GetAsync<Mechanic>("mechanics/1");

        Assert.Equal(1, mechanic.Id);
        Assert.Equal(2, _handler.Requests.Count);
    }

    [Fact]
    public async Task GetAsync_AddsAuthorizationHeader_WhenTokenAvailable()
    {
        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync("the-bearer");

        _handler.EnqueueJson(HttpStatusCode.OK, """{"data":[],"timestamp":"2026-04-29T10:00:00"}""");

        var sut = CreateSut();
        await sut.GetAsync<List<Mechanic>>("mechanics");

        var request = Assert.Single(_handler.Requests);
        Assert.NotNull(request.Headers.Authorization);
        Assert.Equal("Bearer", request.Headers.Authorization!.Scheme);
        Assert.Equal("the-bearer", request.Headers.Authorization.Parameter);
    }

    [Fact]
    public async Task GetAsync_NoTokenAvailable_OmitsAuthorizationHeader()
    {
        _authService.Setup(s => s.GetValidAccessTokenAsync(It.IsAny<CancellationToken>()))
                    .ReturnsAsync((string?)null);

        _handler.EnqueueJson(HttpStatusCode.OK, """{"data":[],"timestamp":"2026-04-29T10:00:00"}""");

        var sut = CreateSut();
        await sut.GetAsync<List<Mechanic>>("mechanics");

        var request = Assert.Single(_handler.Requests);
        Assert.Null(request.Headers.Authorization);
    }
}
