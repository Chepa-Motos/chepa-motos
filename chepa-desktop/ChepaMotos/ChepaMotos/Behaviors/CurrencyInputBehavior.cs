namespace ChepaMotos.Behaviors;

/// <summary>
/// Automatically formats numeric Entry text with period thousand separators
/// (Colombian peso style: 1.250.000). Strips non-digit characters on input,
/// re-formats the display, and preserves the caret at the end.
/// </summary>
public class CurrencyInputBehavior : Behavior<Entry>
{
    private bool _isUpdating;

    protected override void OnAttachedTo(Entry entry)
    {
        base.OnAttachedTo(entry);
        entry.TextChanged += OnTextChanged;

        // Format the initial value if present
        if (!string.IsNullOrEmpty(entry.Text))
            FormatEntry(entry);
    }

    protected override void OnDetachingFrom(Entry entry)
    {
        entry.TextChanged -= OnTextChanged;
        base.OnDetachingFrom(entry);
    }

    private void OnTextChanged(object? sender, TextChangedEventArgs e)
    {
        if (_isUpdating || sender is not Entry entry)
            return;

        FormatEntry(entry);
    }

    private void FormatEntry(Entry entry)
    {
        _isUpdating = true;
        try
        {
            var raw = entry.Text ?? "";

            // Strip everything that isn't a digit
            var digits = new string(raw.Where(char.IsDigit).ToArray());

            // Remove leading zeros (but keep at least one digit)
            digits = digits.TrimStart('0');
            if (digits.Length == 0)
            {
                entry.Text = "";
                return;
            }

            // Parse and format with period as thousands separator
            if (long.TryParse(digits, out var value))
            {
                // Format with commas first, then replace with periods
                entry.Text = value.ToString("N0").Replace(",", ".");
            }
            else
            {
                entry.Text = digits;
            }

            // Move cursor to end
            entry.CursorPosition = entry.Text.Length;
        }
        finally
        {
            _isUpdating = false;
        }
    }

    /// <summary>
    /// Extracts the raw numeric value from a formatted currency string.
    /// "1.250.000" → 1250000m
    /// </summary>
    public static decimal GetValue(string? formattedText)
    {
        if (string.IsNullOrWhiteSpace(formattedText))
            return 0m;

        var digits = new string(formattedText.Where(char.IsDigit).ToArray());
        return decimal.TryParse(digits, out var value) ? value : 0m;
    }
}
