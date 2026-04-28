using ChepaMotos.Models;
using ChepaMotos.Models.Requests;
using ChepaMotos.Services.Api;

namespace ChepaMotos.Services.Domain;

public sealed class MechanicService : IMechanicService
{
    private readonly IApiClient _api;

    public MechanicService(IApiClient api)
    {
        _api = api;
    }

    public async Task<IReadOnlyList<Mechanic>> ListAsync(bool active, CancellationToken ct = default)
    {
        var query = new Dictionary<string, string?>
        {
            ["active"] = active ? "true" : "false",
        };
        var data = await _api.GetAsync<List<Mechanic>>("mechanics", query, ct);
        return data;
    }

    public async Task<IReadOnlyList<Mechanic>> ListAllAsync(CancellationToken ct = default)
    {
        var activeTask = ListAsync(active: true, ct);
        var inactiveTask = ListAsync(active: false, ct);
        await Task.WhenAll(activeTask, inactiveTask);
        return activeTask.Result.Concat(inactiveTask.Result).ToList();
    }

    public Task<Mechanic> GetByIdAsync(long id, CancellationToken ct = default)
        => _api.GetAsync<Mechanic>($"mechanics/{id}", query: null, ct);

    public Task<Mechanic> CreateAsync(string name, CancellationToken ct = default)
        => _api.PostAsync<Mechanic, CreateMechanicRequest>(
            "mechanics",
            new CreateMechanicRequest { Name = name },
            ct);

    public Task<Mechanic> UpdateStatusAsync(long id, bool isActive, CancellationToken ct = default)
        => _api.PatchAsync<Mechanic, UpdateMechanicStatusRequest>(
            $"mechanics/{id}/status",
            new UpdateMechanicStatusRequest { IsActive = isActive },
            ct);
}
