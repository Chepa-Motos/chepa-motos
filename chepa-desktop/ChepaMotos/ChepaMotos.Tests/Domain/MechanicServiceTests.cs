using ChepaMotos.Models;
using ChepaMotos.Models.Requests;
using ChepaMotos.Services.Api;
using ChepaMotos.Services.Domain;
using Moq;
using Xunit;

namespace ChepaMotos.Tests.Domain;

public class MechanicServiceTests
{
    private readonly Mock<IApiClient> _api = new();

    private MechanicService CreateSut() => new(_api.Object);

    [Fact]
    public async Task ListAsync_PassesActiveFlagAsQuery()
    {
        IDictionary<string, string?>? capturedQuery = null;
        _api.Setup(a => a.GetAsync<List<Mechanic>>(
                "mechanics",
                It.IsAny<IDictionary<string, string?>?>(),
                It.IsAny<CancellationToken>()))
            .Callback<string, IDictionary<string, string?>?, CancellationToken>((_, q, _) => capturedQuery = q)
            .ReturnsAsync([]);

        var sut = CreateSut();
        await sut.ListAsync(active: false);

        Assert.NotNull(capturedQuery);
        Assert.Equal("false", capturedQuery!["active"]);
    }

    [Fact]
    public async Task ListAllAsync_QueriesActiveAndInactive_AndConcatenates()
    {
        var actives = new List<Mechanic> { new() { Id = 1, Name = "Pedro", IsActive = true } };
        var inactives = new List<Mechanic> { new() { Id = 2, Name = "Luis", IsActive = false } };

        _api.Setup(a => a.GetAsync<List<Mechanic>>(
                "mechanics",
                It.Is<IDictionary<string, string?>?>(q => q != null && q["active"] == "true"),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(actives);
        _api.Setup(a => a.GetAsync<List<Mechanic>>(
                "mechanics",
                It.Is<IDictionary<string, string?>?>(q => q != null && q["active"] == "false"),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(inactives);

        var sut = CreateSut();
        var all = await sut.ListAllAsync();

        Assert.Equal(2, all.Count);
        Assert.Contains(all, m => m.Id == 1);
        Assert.Contains(all, m => m.Id == 2);
    }

    [Fact]
    public async Task CreateAsync_PostsNameInRequestBody()
    {
        CreateMechanicRequest? captured = null;
        _api.Setup(a => a.PostAsync<Mechanic, CreateMechanicRequest>(
                "mechanics",
                It.IsAny<CreateMechanicRequest>(),
                It.IsAny<CancellationToken>()))
            .Callback<string, CreateMechanicRequest, CancellationToken>((_, req, _) => captured = req)
            .ReturnsAsync(new Mechanic { Id = 99, Name = "Nuevo", IsActive = true });

        var sut = CreateSut();
        var created = await sut.CreateAsync("Nuevo");

        Assert.NotNull(captured);
        Assert.Equal("Nuevo", captured!.Name);
        Assert.Equal(99, created.Id);
    }

    [Fact]
    public async Task UpdateStatusAsync_PatchesCorrectUrlWithBody()
    {
        UpdateMechanicStatusRequest? captured = null;
        _api.Setup(a => a.PatchAsync<Mechanic, UpdateMechanicStatusRequest>(
                "mechanics/42/status",
                It.IsAny<UpdateMechanicStatusRequest>(),
                It.IsAny<CancellationToken>()))
            .Callback<string, UpdateMechanicStatusRequest, CancellationToken>((_, req, _) => captured = req)
            .ReturnsAsync(new Mechanic { Id = 42, Name = "X", IsActive = false });

        var sut = CreateSut();
        await sut.UpdateStatusAsync(42, isActive: false);

        Assert.NotNull(captured);
        Assert.False(captured!.IsActive);
    }
}
