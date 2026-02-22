using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

public class Invoice
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("invoice_type")]
    public string InvoiceType { get; set; } = string.Empty;

    [JsonPropertyName("mechanic")]
    public Mechanic? Mechanic { get; set; }

    [JsonPropertyName("vehicle")]
    public Vehicle? Vehicle { get; set; }

    [JsonPropertyName("buyer_name")]
    public string? BuyerName { get; set; }

    [JsonPropertyName("created_at")]
    public DateTime CreatedAt { get; set; }

    [JsonPropertyName("labor_amount")]
    public decimal LaborAmount { get; set; }

    [JsonPropertyName("total_amount")]
    public decimal TotalAmount { get; set; }

    [JsonPropertyName("is_cancelled")]
    public bool IsCancelled { get; set; }

    [JsonPropertyName("items")]
    public List<InvoiceItem> Items { get; set; } = [];
}
