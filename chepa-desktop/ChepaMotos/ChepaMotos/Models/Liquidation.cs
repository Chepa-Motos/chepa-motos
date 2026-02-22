using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

public class Liquidation
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("mechanic")]
    public Mechanic Mechanic { get; set; } = default!;

    [JsonPropertyName("date")]
    public string Date { get; set; } = string.Empty;

    [JsonPropertyName("invoice_count")]
    public int InvoiceCount { get; set; }

    [JsonPropertyName("total_revenue")]
    public decimal TotalRevenue { get; set; }

    [JsonPropertyName("mechanic_share")]
    public decimal MechanicShare { get; set; }

    [JsonPropertyName("shop_share")]
    public decimal ShopShare { get; set; }
}
