using System.Text;
using System.Text.Json;
using ChepaMotos.Services.Auth;
using Xunit;

namespace ChepaMotos.Tests.Auth;

public class JwtClaimsParserTests
{
    [Fact]
    public void Parse_ValidToken_ReturnsUsernameAndRole()
    {
        var jwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "admin",
            ["role"] = "GERENTE",
        });

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.Equal("admin", claims.Username);
        Assert.Single(claims.Roles);
        Assert.Equal("GERENTE", claims.Roles[0]);
    }

    [Fact]
    public void Parse_TokenWithoutSub_ReturnsNullUsername()
    {
        var jwt = BuildJwt(new Dictionary<string, object> { ["role"] = "GERENTE" });

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.Null(claims.Username);
        Assert.Single(claims.Roles);
    }

    [Fact]
    public void Parse_TokenWithRolesArray_ReturnsAllRoles()
    {
        var jwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["roles"] = new[] { "ADMIN", "USER" },
        });

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.Equal(2, claims.Roles.Count);
        Assert.Contains("ADMIN", claims.Roles);
        Assert.Contains("USER", claims.Roles);
    }

    [Fact]
    public void Parse_TokenWithRolePrefix_NormalizesToBareName()
    {
        var jwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["role"] = "ROLE_GERENTE",
        });

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.Equal("GERENTE", claims.Roles[0]);
    }

    [Fact]
    public void Parse_TokenWithAuthorities_ReadsAsRoles()
    {
        // Spring Security a veces serializa las roles bajo "authorities".
        // El parser tolera ese alias.
        var jwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["authorities"] = new[] { "ROLE_GERENTE" },
        });

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.Equal("GERENTE", claims.Roles[0]);
    }

    [Fact]
    public void Parse_TokenWithExp_ReturnsExpiresAt()
    {
        var expAt = DateTimeOffset.UtcNow.AddMinutes(15);
        var jwt = BuildJwt(new Dictionary<string, object>
        {
            ["sub"] = "user",
            ["exp"] = expAt.ToUnixTimeSeconds(),
        });

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.NotNull(claims.ExpiresAt);
        Assert.Equal(expAt.ToUnixTimeSeconds(), claims.ExpiresAt.Value.ToUnixTimeSeconds());
    }

    [Fact]
    public void Parse_TokenWithoutExp_ReturnsNullExpiresAt()
    {
        var jwt = BuildJwt(new Dictionary<string, object> { ["sub"] = "user" });

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.Null(claims.ExpiresAt);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("not-a-jwt")]
    [InlineData("only.two")]
    public void Parse_InvalidInput_ReturnsEmptyClaims(string? input)
    {
        var claims = JwtClaimsParser.Parse(input);

        Assert.Null(claims.Username);
        Assert.Empty(claims.Roles);
        Assert.Null(claims.ExpiresAt);
    }

    [Fact]
    public void Parse_MalformedPayload_ReturnsEmptyClaims()
    {
        // Header válido + payload con JSON inválido + firma fake.
        var jwt = "eyJhbGciOiJIUzI1NiJ9.bm90LWpzb24.fakesig";

        var claims = JwtClaimsParser.Parse(jwt);

        Assert.Null(claims.Username);
        Assert.Empty(claims.Roles);
        Assert.Null(claims.ExpiresAt);
    }

    /// <summary>
    /// Construye un JWT con header fijo + payload serializado en Base64URL +
    /// firma falsa. La firma no se valida (el parser solo lee el payload).
    /// </summary>
    private static string BuildJwt(IDictionary<string, object> payload)
    {
        const string header = "eyJhbGciOiJIUzI1NiJ9"; // {"alg":"HS256"}
        var payloadJson = JsonSerializer.Serialize(payload);
        var payloadEncoded = Base64UrlEncode(Encoding.UTF8.GetBytes(payloadJson));
        return $"{header}.{payloadEncoded}.fakesig";
    }

    private static string Base64UrlEncode(byte[] bytes)
        => Convert.ToBase64String(bytes)
            .Replace('+', '-')
            .Replace('/', '_')
            .TrimEnd('=');
}
