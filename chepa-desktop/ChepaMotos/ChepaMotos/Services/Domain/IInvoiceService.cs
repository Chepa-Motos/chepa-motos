using ChepaMotos.Models;
using ChepaMotos.Models.Requests;

namespace ChepaMotos.Services.Domain;

public interface IInvoiceService
{
    /// <summary>
    /// GET /invoices con filtros opcionales. El parámetro <paramref name="cancelled"/>
    /// tiene default <c>false</c> en el backend — pasar <c>true</c> trae solo
    /// canceladas; no existe modo "ambas" en una sola llamada.
    /// </summary>
    Task<IReadOnlyList<Invoice>> ListAsync(
        DateTime? date = null,
        string? type = null,
        long? mechanicId = null,
        bool cancelled = false,
        CancellationToken ct = default);

    /// <summary>GET /invoices/{id}.</summary>
    Task<Invoice> GetByIdAsync(long id, CancellationToken ct = default);

    /// <summary>POST /invoices/service.</summary>
    Task<Invoice> CreateServiceAsync(
        CreateServiceInvoiceRequest request,
        CancellationToken ct = default);

    /// <summary>POST /invoices/delivery.</summary>
    Task<Invoice> CreateDeliveryAsync(
        CreateDeliveryInvoiceRequest request,
        CancellationToken ct = default);

    /// <summary>PATCH /invoices/{id}/cancel — irreversible.</summary>
    Task<InvoiceCancelResult> CancelAsync(long id, CancellationToken ct = default);
}
