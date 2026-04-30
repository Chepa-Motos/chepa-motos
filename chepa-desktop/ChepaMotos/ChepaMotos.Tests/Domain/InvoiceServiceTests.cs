using ChepaMotos.Models;
using ChepaMotos.Models.Requests;
using ChepaMotos.Services.Api;
using ChepaMotos.Services.Domain;
using Moq;
using Xunit;

namespace ChepaMotos.Tests.Domain;

public class InvoiceServiceTests
{
    private readonly Mock<IApiClient> _api = new();

    private InvoiceService CreateSut() => new(_api.Object);

    [Fact]
    public async Task ListAsync_BuildsExpectedQueryParams()
    {
        IDictionary<string, string?>? captured = null;
        _api.Setup(a => a.GetAsync<List<Invoice>>(
                "invoices",
                It.IsAny<IDictionary<string, string?>?>(),
                It.IsAny<CancellationToken>()))
            .Callback<string, IDictionary<string, string?>?, CancellationToken>((_, q, _) => captured = q)
            .ReturnsAsync([]);

        var sut = CreateSut();
        await sut.ListAsync(
            date: new DateTime(2026, 4, 29),
            type: "SERVICE",
            mechanicId: 7,
            cancelled: true);

        Assert.NotNull(captured);
        Assert.Equal("2026-04-29", captured!["date"]);
        Assert.Equal("SERVICE", captured["type"]);
        Assert.Equal("7", captured["mechanic_id"]);
        Assert.Equal("true", captured["cancelled"]);
    }

    [Fact]
    public async Task ListAsync_OmitsNullFilters()
    {
        IDictionary<string, string?>? captured = null;
        _api.Setup(a => a.GetAsync<List<Invoice>>(
                "invoices",
                It.IsAny<IDictionary<string, string?>?>(),
                It.IsAny<CancellationToken>()))
            .Callback<string, IDictionary<string, string?>?, CancellationToken>((_, q, _) => captured = q)
            .ReturnsAsync([]);

        var sut = CreateSut();
        await sut.ListAsync();

        Assert.NotNull(captured);
        Assert.Equal("false", captured!["cancelled"]);
        Assert.False(captured.ContainsKey("date"));
        Assert.False(captured.ContainsKey("type"));
        Assert.False(captured.ContainsKey("mechanic_id"));
    }

    [Fact]
    public async Task CreateServiceAsync_PostsRequestVerbatim()
    {
        // Regression test: el bug histórico era que el campo se llamaba `plate` en
        // lugar de `vehicle_plate`. Aseguramos que el request llega tal cual sin
        // que el servicio lo modifique.
        CreateServiceInvoiceRequest? captured = null;
        _api.Setup(a => a.PostAsync<Invoice, CreateServiceInvoiceRequest>(
                "invoices/service",
                It.IsAny<CreateServiceInvoiceRequest>(),
                It.IsAny<CancellationToken>()))
            .Callback<string, CreateServiceInvoiceRequest, CancellationToken>((_, req, _) => captured = req)
            .ReturnsAsync(new Invoice { Id = 1 });

        var request = new CreateServiceInvoiceRequest
        {
            MechanicId = 1,
            VehiclePlate = "ABC12X",
            Model = "Boxer 150 2022",
            LaborAmount = 50000,
            Items = [new CreateInvoiceItemRequest { Description = "Cambio de aceite", Quantity = 1, UnitPrice = 30000 }],
        };

        var sut = CreateSut();
        await sut.CreateServiceAsync(request);

        Assert.Same(request, captured);
        Assert.Equal("ABC12X", captured!.VehiclePlate);
    }

    [Fact]
    public async Task CancelAsync_PatchesCorrectPath()
    {
        _api.Setup(a => a.PatchAsync<InvoiceCancelResult>(
                "invoices/123/cancel",
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(new InvoiceCancelResult { Id = 123, IsCancelled = true });

        var sut = CreateSut();
        var result = await sut.CancelAsync(123);

        Assert.Equal(123, result.Id);
        Assert.True(result.IsCancelled);
        _api.Verify(a => a.PatchAsync<InvoiceCancelResult>("invoices/123/cancel", It.IsAny<CancellationToken>()), Times.Once);
    }
}
