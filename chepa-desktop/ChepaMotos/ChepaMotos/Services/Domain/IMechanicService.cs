using ChepaMotos.Models;

namespace ChepaMotos.Services.Domain;

public interface IMechanicService
{
    /// <summary>GET /mechanics?active={active}.</summary>
    Task<IReadOnlyList<Mechanic>> ListAsync(bool active, CancellationToken ct = default);

    /// <summary>
    /// Activos + inactivos en una sola lista. El backend no soporta "todos"
    /// en una llamada, así que esto hace dos requests en paralelo.
    /// </summary>
    Task<IReadOnlyList<Mechanic>> ListAllAsync(CancellationToken ct = default);

    /// <summary>GET /mechanics/{id}.</summary>
    Task<Mechanic> GetByIdAsync(long id, CancellationToken ct = default);

    /// <summary>POST /mechanics — requiere rol GERENTE (403 FORBIDDEN si no).</summary>
    Task<Mechanic> CreateAsync(string name, CancellationToken ct = default);

    /// <summary>PATCH /mechanics/{id}/status — requiere rol GERENTE.</summary>
    Task<Mechanic> UpdateStatusAsync(long id, bool isActive, CancellationToken ct = default);
}
