using ChepaMotos.Models;
using ChepaMotos.Models.Requests;
using ChepaMotos.Services.Api;

namespace ChepaMotos.Services.Domain;

public sealed class InvoiceService : IInvoiceService
{
    private readonly IApiClient _api;

    public InvoiceService(IApiClient api)
    {
        _api = api;
    }

    public async Task<IReadOnlyList<Invoice>> ListAsync(
        DateTime? date = null,
        string? type = null,
        long? mechanicId = null,
        bool cancelled = false,
        CancellationToken ct = default)
    {
        var query = new Dictionary<string, string?>
        {
            ["cancelled"] = cancelled ? "true" : "false",
        };
        if (date is DateTime d)
            query["date"] = d.ToString("yyyy-MM-dd");
        if (!string.IsNullOrEmpty(type))
            query["type"] = type;
        if (mechanicId is long mid)
            query["mechanic_id"] = mid.ToString();

        return await _api.GetAsync<List<Invoice>>("invoices", query, ct);
    }

    public Task<Invoice> GetByIdAsync(long id, CancellationToken ct = default)
        => _api.GetAsync<Invoice>($"invoices/{id}", query: null, ct);

    public Task<Invoice> CreateServiceAsync(CreateServiceInvoiceRequest request, CancellationToken ct = default)
        => _api.PostAsync<Invoice, CreateServiceInvoiceRequest>("invoices/service", request, ct);

    public Task<Invoice> CreateDeliveryAsync(CreateDeliveryInvoiceRequest request, CancellationToken ct = default)
        => _api.PostAsync<Invoice, CreateDeliveryInvoiceRequest>("invoices/delivery", request, ct);

    public Task<InvoiceCancelResult> CancelAsync(long id, CancellationToken ct = default)
        => _api.PatchAsync<InvoiceCancelResult>($"invoices/{id}/cancel", ct);
}
