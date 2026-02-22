using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

/// <summary>
/// Standard success response envelope from the API.
/// </summary>
public class ApiResponse<T>
{
    [JsonPropertyName("data")]
    public T Data { get; set; } = default!;

    [JsonPropertyName("timestamp")]
    public string Timestamp { get; set; } = string.Empty;
}

/// <summary>
/// Standard error response envelope from the API.
/// </summary>
public class ApiErrorResponse
{
    [JsonPropertyName("code")]
    public string Code { get; set; } = string.Empty;

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("timestamp")]
    public string Timestamp { get; set; } = string.Empty;
}
