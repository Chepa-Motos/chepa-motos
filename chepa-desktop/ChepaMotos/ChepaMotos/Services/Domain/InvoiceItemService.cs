using ChepaMotos.Models;
using ChepaMotos.Services.Api;

namespace ChepaMotos.Services.Domain;

public sealed class InvoiceItemService : IInvoiceItemService
{
    private readonly IApiClient _api;

    public InvoiceItemService(IApiClient api)
    {
        _api = api;
    }

    public async Task<IReadOnlyList<ItemSuggestion>> GetSuggestionsAsync(
        string model,
        string query,
        CancellationToken ct = default)
    {
        var trimmedModel = model?.Trim() ?? string.Empty;
        var trimmedQuery = query?.Trim() ?? string.Empty;

        if (string.IsNullOrEmpty(trimmedModel) || trimmedQuery.Length < 2)
            return Array.Empty<ItemSuggestion>();

        var data = await _api.GetAsync<List<ItemSuggestion>>(
            "invoice-items/suggestions",
            new Dictionary<string, string?>
            {
                ["model"] = trimmedModel,
                ["q"] = trimmedQuery,
            },
            ct);

        return data;
    }
}
