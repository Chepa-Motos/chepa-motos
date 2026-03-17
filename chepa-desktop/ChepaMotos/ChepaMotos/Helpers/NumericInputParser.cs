namespace ChepaMotos.Helpers;

public static class NumericInputParser
{
    public static decimal ParseDecimal(string? text)
    {
        if (string.IsNullOrWhiteSpace(text)) return 0m;

        // Keep only numeric separators and digits.
        var cleaned = new string(text.Where(c => char.IsDigit(c) || c == ',' || c == '.').ToArray());

        if (cleaned.Contains(','))
        {
            // Treat comma as decimal separator and dots as thousands separators.
            cleaned = cleaned.Replace(".", "").Replace(",", ".");
        }
        else
        {
            // If only dots are present, decide thousands vs decimal.
            var periodCount = cleaned.Count(c => c == '.');
            if (periodCount > 1)
            {
                cleaned = cleaned.Replace(".", "");
            }
            else if (periodCount == 1)
            {
                var afterDot = cleaned[(cleaned.IndexOf('.') + 1)..];
                if (afterDot.Length == 3)
                    cleaned = cleaned.Replace(".", "");
            }
        }

        return decimal.TryParse(
            cleaned,
            System.Globalization.NumberStyles.Any,
            System.Globalization.CultureInfo.InvariantCulture,
            out var value)
            ? value
            : 0m;
    }
}