using System.Text.Json.Serialization;

namespace ChepaMotos.Models.Requests;

/// <summary>
/// POST /mechanics
/// </summary>
public class CreateMechanicRequest
{
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>
/// PATCH /mechanics/{id}/status
/// </summary>
public class UpdateMechanicStatusRequest
{
    [JsonPropertyName("is_active")]
    public bool IsActive { get; set; }
}
