using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

public class ItemSuggestion
{
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    [JsonPropertyName("unit_price")]
    public decimal UnitPrice { get; set; }
}
