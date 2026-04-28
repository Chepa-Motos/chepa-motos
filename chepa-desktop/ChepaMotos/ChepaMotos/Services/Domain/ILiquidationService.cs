using ChepaMotos.Models;

namespace ChepaMotos.Services.Domain;

public interface ILiquidationService
{
    /// <summary>GET /liquidations con filtros opcionales.</summary>
    Task<IReadOnlyList<Liquidation>> ListAsync(
        DateTime? date = null,
        long? mechanicId = null,
        CancellationToken ct = default);

    /// <summary>
    /// POST /liquidations — requiere rol GERENTE. Si <paramref name="mechanicId"/>
    /// es null, el backend liquida a todos los mecánicos activos del día.
    /// La respuesta es siempre una lista (uno o más mecánicos liquidados).
    /// </summary>
    Task<IReadOnlyList<Liquidation>> CreateAsync(
        DateTime date,
        long? mechanicId = null,
        CancellationToken ct = default);
}
