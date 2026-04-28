using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

/// <summary>
/// Respuesta de PATCH /invoices/{id}/cancel — solo trae id + flag, no la factura completa.
/// </summary>
public class InvoiceCancelResult
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("is_cancelled")]
    public bool IsCancelled { get; set; }
}
