using ChepaMotos.Models;

namespace ChepaMotos.Services.Domain;

public interface IInvoiceItemService
{
    /// <summary>
    /// GET /invoice-items/suggestions?model={model}&q={query}. El backend valida
    /// que <c>q</c> tenga al menos 2 caracteres; cortocircuitamos antes de
    /// pegarle a la red para no dispararle un VALIDATION_ERROR por cada tecla.
    /// </summary>
    Task<IReadOnlyList<ItemSuggestion>> GetSuggestionsAsync(
        string model,
        string query,
        CancellationToken ct = default);
}
