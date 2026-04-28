using System.Text.Json.Serialization;

namespace ChepaMotos.Models;

/// <summary>
/// Response from POST /auth/login and POST /auth/refresh.
/// AuthTokensResponse en el backend NO declara @JsonProperty, así que sale en camelCase
/// (a diferencia del resto de DTOs que usan snake_case).
/// </summary>
public class AuthTokens
{
    [JsonPropertyName("tokenType")]
    public string TokenType { get; set; } = "Bearer";

    [JsonPropertyName("accessToken")]
    public string AccessToken { get; set; } = string.Empty;

    [JsonPropertyName("expiresIn")]
    public long ExpiresIn { get; set; }

    [JsonPropertyName("refreshToken")]
    public string RefreshToken { get; set; } = string.Empty;
}

/// <summary>POST /auth/login body.</summary>
public class LoginRequest
{
    [JsonPropertyName("username")]
    public string Username { get; set; } = string.Empty;

    [JsonPropertyName("password")]
    public string Password { get; set; } = string.Empty;
}

/// <summary>POST /auth/refresh and POST /auth/logout body.</summary>
public class RefreshTokenRequest
{
    [JsonPropertyName("refresh_token")]
    public string RefreshToken { get; set; } = string.Empty;
}
