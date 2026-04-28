using ChepaMotos.Models;
using ChepaMotos.Models.Requests;
using ChepaMotos.Services.Api;

namespace ChepaMotos.Services.Domain;

public sealed class LiquidationService : ILiquidationService
{
    private readonly IApiClient _api;

    public LiquidationService(IApiClient api)
    {
        _api = api;
    }

    public async Task<IReadOnlyList<Liquidation>> ListAsync(
        DateTime? date = null,
        long? mechanicId = null,
        CancellationToken ct = default)
    {
        var query = new Dictionary<string, string?>();
        if (date is DateTime d)
            query["date"] = d.ToString("yyyy-MM-dd");
        if (mechanicId is long mid)
            query["mechanic_id"] = mid.ToString();

        return await _api.GetAsync<List<Liquidation>>("liquidations", query, ct);
    }

    public async Task<IReadOnlyList<Liquidation>> CreateAsync(
        DateTime date,
        long? mechanicId = null,
        CancellationToken ct = default)
    {
        var request = new CreateLiquidationRequest
        {
            Date = date.ToString("yyyy-MM-dd"),
            MechanicId = mechanicId,
        };

        return await _api.PostAsync<List<Liquidation>, CreateLiquidationRequest>(
            "liquidations",
            request,
            ct);
    }
}
