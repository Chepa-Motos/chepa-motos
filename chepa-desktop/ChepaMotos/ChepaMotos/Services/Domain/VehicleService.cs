using ChepaMotos.Models;
using ChepaMotos.Services.Api;

namespace ChepaMotos.Services.Domain;

public sealed class VehicleService : IVehicleService
{
    private readonly IApiClient _api;

    public VehicleService(IApiClient api)
    {
        _api = api;
    }

    public async Task<Vehicle?> GetByPlateAsync(string plate, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(plate))
            return null;

        // El backend ya normaliza con trim+uppercase, pero limpiamos espacios para
        // no enviar URLs feas con %20.
        var normalized = plate.Trim();

        try
        {
            return await _api.GetAsync<Vehicle>(
                $"vehicles/plate/{Uri.EscapeDataString(normalized)}",
                query: null,
                ct);
        }
        catch (ApiException ex) when (ex.Code == ApiErrorCodes.VehicleNotFound)
        {
            return null;
        }
    }
}
