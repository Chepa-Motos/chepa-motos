using System.Text;
using System.Text.Json;

namespace ChepaMotos.Services.Auth;

/// <summary>
/// Decodifica el payload de un JWT (Base64URL) sin validar la firma — el backend
/// es la autoridad de verificación; el cliente solo necesita leer claims.
///
/// Backend (JwtAccessTokenService): emite claims <c>sub</c> (username) y <c>role</c>
/// (string único, ej. "GERENTE"). Mantenemos un parser tolerante por si en el
/// futuro pasan a un array de roles.
/// </summary>
public static class JwtClaimsParser
{
    public readonly record struct JwtClaims(
        string? Username,
        IReadOnlyList<string> Roles,
        DateTimeOffset? ExpiresAt);

    public static JwtClaims Parse(string? jwt)
    {
        if (string.IsNullOrWhiteSpace(jwt))
            return new JwtClaims(null, Array.Empty<string>(), null);

        var parts = jwt.Split('.');
        if (parts.Length < 2)
            return new JwtClaims(null, Array.Empty<string>(), null);

        try
        {
            var payloadBytes = Base64UrlDecode(parts[1]);
            using var doc = JsonDocument.Parse(payloadBytes);
            var root = doc.RootElement;

            string? username = ReadString(root, "sub");

            var roles = new List<string>();
            AppendRoles(root, "role", roles);
            AppendRoles(root, "roles", roles);
            AppendRoles(root, "authorities", roles);

            DateTimeOffset? expiresAt = null;
            if (root.TryGetProperty("exp", out var exp)
                && exp.ValueKind == JsonValueKind.Number
                && exp.TryGetInt64(out var expSeconds))
            {
                expiresAt = DateTimeOffset.FromUnixTimeSeconds(expSeconds);
            }

            return new JwtClaims(username, roles, expiresAt);
        }
        catch
        {
            return new JwtClaims(null, Array.Empty<string>(), null);
        }
    }

    private static void AppendRoles(JsonElement root, string claim, List<string> sink)
    {
        if (!root.TryGetProperty(claim, out var element))
            return;

        switch (element.ValueKind)
        {
            case JsonValueKind.String:
                var value = element.GetString();
                if (!string.IsNullOrWhiteSpace(value))
                    sink.Add(NormalizeRole(value));
                break;

            case JsonValueKind.Array:
                foreach (var item in element.EnumerateArray())
                {
                    if (item.ValueKind == JsonValueKind.String)
                    {
                        var s = item.GetString();
                        if (!string.IsNullOrWhiteSpace(s))
                            sink.Add(NormalizeRole(s));
                    }
                }
                break;
        }
    }

    /// <summary>Acepta "GERENTE" o "ROLE_GERENTE" y devuelve "GERENTE".</summary>
    private static string NormalizeRole(string raw)
    {
        var trimmed = raw.Trim();
        return trimmed.StartsWith("ROLE_", StringComparison.OrdinalIgnoreCase)
            ? trimmed[5..]
            : trimmed;
    }

    private static string? ReadString(JsonElement root, string property)
    {
        if (root.TryGetProperty(property, out var element) && element.ValueKind == JsonValueKind.String)
            return element.GetString();
        return null;
    }

    private static byte[] Base64UrlDecode(string input)
    {
        var s = input.Replace('-', '+').Replace('_', '/');
        switch (s.Length % 4)
        {
            case 2: s += "=="; break;
            case 3: s += "="; break;
        }
        return Convert.FromBase64String(s);
    }
}
