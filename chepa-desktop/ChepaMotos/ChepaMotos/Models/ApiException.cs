namespace ChepaMotos.Models;

/// <summary>
/// Excepción tipada que envuelve los errores de la API.
/// El backend devuelve siempre <see cref="ApiErrorResponse"/> para 4xx/5xx con un
/// <c>code</c> estable (ej. INVALID_CREDENTIALS, AUTH_REQUIRED, SESSION_EXPIRED,
/// FORBIDDEN, VALIDATION_ERROR, MECHANIC_NOT_FOUND, VEHICLE_NOT_FOUND,
/// INVOICE_NOT_FOUND, INVOICE_ALREADY_CANCELLED, LIQUIDATION_ALREADY_EXISTS,
/// INTERNAL_ERROR).
/// </summary>
public sealed class ApiException : Exception
{
    public string Code { get; }
    public int StatusCode { get; }

    public ApiException(string code, string message, int statusCode)
        : base(message)
    {
        Code = code;
        StatusCode = statusCode;
    }
}

public static class ApiErrorCodes
{
    public const string InvalidCredentials = "INVALID_CREDENTIALS";
    public const string AuthRequired = "AUTH_REQUIRED";
    public const string SessionExpired = "SESSION_EXPIRED";
    public const string Forbidden = "FORBIDDEN";
    public const string ValidationError = "VALIDATION_ERROR";
    public const string MechanicNotFound = "MECHANIC_NOT_FOUND";
    public const string VehicleNotFound = "VEHICLE_NOT_FOUND";
    public const string InvoiceNotFound = "INVOICE_NOT_FOUND";
    public const string InvoiceAlreadyCancelled = "INVOICE_ALREADY_CANCELLED";
    public const string LiquidationAlreadyExists = "LIQUIDATION_ALREADY_EXISTS";
    public const string InternalError = "INTERNAL_ERROR";
}
