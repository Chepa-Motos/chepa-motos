using System.Text.Json.Serialization;

namespace ChepaMotos.Models.Requests;

/// <summary>
/// POST /liquidations
/// </summary>
public class CreateLiquidationRequest
{
    [JsonPropertyName("date")]
    public string Date { get; set; } = string.Empty;

    [JsonPropertyName("mechanic_id")]
    public long? MechanicId { get; set; }
}
