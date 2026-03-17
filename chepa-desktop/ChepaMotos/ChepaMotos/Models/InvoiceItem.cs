using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

public class InvoiceItem
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    [JsonPropertyName("quantity")]
    public decimal Quantity { get; set; }

    [JsonPropertyName("unit_price")]
    public decimal UnitPrice { get; set; }

    [JsonPropertyName("subtotal")]
    public decimal Subtotal { get; set; }
}
