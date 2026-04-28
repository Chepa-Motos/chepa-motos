using ChepaMotos.Models;

namespace ChepaMotos.Services.Domain;

public interface IVehicleService
{
    /// <summary>
    /// GET /vehicles/plate/{plate}. Devuelve <c>null</c> si el backend responde
    /// 404 con <c>VEHICLE_NOT_FOUND</c> — placa no registrada es un caso esperado
    /// (al crear factura SERVICE con vehículo nuevo). Otros errores se propagan.
    /// </summary>
    Task<Vehicle?> GetByPlateAsync(string plate, CancellationToken ct = default);
}
