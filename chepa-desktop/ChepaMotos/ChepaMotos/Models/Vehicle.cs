using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

public class Vehicle
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("plate")]
    public string Plate { get; set; } = string.Empty;

    [JsonPropertyName("model")]
    public string Model { get; set; } = string.Empty;
}
