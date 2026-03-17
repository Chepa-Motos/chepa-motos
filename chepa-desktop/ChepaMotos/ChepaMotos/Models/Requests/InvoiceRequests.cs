using System.Text.Json.Serialization;

namespace ChepaMotos.Models.Requests;

/// <summary>
/// Item payload used inside service and delivery invoice requests.
/// </summary>
public class CreateInvoiceItemRequest
{
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    [JsonPropertyName("quantity")]
    public decimal Quantity { get; set; }

    [JsonPropertyName("unit_price")]
    public decimal UnitPrice { get; set; }
}

/// <summary>
/// POST /invoices/service
/// </summary>
public class CreateServiceInvoiceRequest
{
    [JsonPropertyName("mechanic_id")]
    public long MechanicId { get; set; }

    [JsonPropertyName("plate")]
    public string Plate { get; set; } = string.Empty;

    [JsonPropertyName("model")]
    public string Model { get; set; } = string.Empty;

    [JsonPropertyName("labor_amount")]
    public decimal LaborAmount { get; set; }

    [JsonPropertyName("items")]
    public List<CreateInvoiceItemRequest> Items { get; set; } = [];
}

/// <summary>
/// POST /invoices/delivery
/// </summary>
public class CreateDeliveryInvoiceRequest
{
    [JsonPropertyName("buyer_name")]
    public string BuyerName { get; set; } = string.Empty;

    [JsonPropertyName("items")]
    public List<CreateInvoiceItemRequest> Items { get; set; } = [];
}
