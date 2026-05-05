namespace ChepaMotos.Helpers;

/// <summary>
/// Formato de moneda consistente con todo el proyecto. Convención COP:
/// símbolo <c>$</c>, sin decimales, punto como separador de miles
/// (<c>$120.000</c>).
///
/// Para parsear input del usuario, usar
/// <see cref="ChepaMotos.Behaviors.CurrencyInputBehavior.GetValue"/>.
/// </summary>
public static class CurrencyFormatter
{
    public static string Format(decimal value)
        => $"${value:N0}".Replace(",", ".");
}
