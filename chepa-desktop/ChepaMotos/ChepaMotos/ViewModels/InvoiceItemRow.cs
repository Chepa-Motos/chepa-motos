using System.ComponentModel;
using ChepaMotos.Behaviors;

namespace ChepaMotos.ViewModels;

/// <summary>
/// Observable row model for an invoice line item.
/// Quantity and UnitPrice are stored as formatted strings (with thousand separators)
/// and the subtotal is recalculated automatically.
/// </summary>
public class InvoiceItemRow : INotifyPropertyChanged
{
    private string _quantity = "1";
    private string _description = "";
    private string _unitPrice = "";

    public event PropertyChangedEventHandler? PropertyChanged;
    /// <summary>Raised whenever the subtotal of any row changes, so the parent can recalculate the total.</summary>
    public event EventHandler? SubtotalChanged;

    public string Quantity
    {
        get => _quantity;
        set
        {
            if (_quantity == value) return;
            _quantity = value;
            OnPropertyChanged(nameof(Quantity));
            OnPropertyChanged(nameof(Subtotal));
            OnPropertyChanged(nameof(SubtotalFormatted));
            SubtotalChanged?.Invoke(this, EventArgs.Empty);
        }
    }

    public string Description
    {
        get => _description;
        set
        {
            if (_description == value) return;
            _description = value;
            OnPropertyChanged(nameof(Description));
        }
    }

    public string UnitPrice
    {
        get => _unitPrice;
        set
        {
            if (_unitPrice == value) return;
            _unitPrice = value;
            OnPropertyChanged(nameof(UnitPrice));
            OnPropertyChanged(nameof(Subtotal));
            OnPropertyChanged(nameof(SubtotalFormatted));
            SubtotalChanged?.Invoke(this, EventArgs.Empty);
        }
    }

    /// <summary>Raw decimal subtotal = parsed quantity × parsed unit price.</summary>
    public decimal Subtotal
    {
        get
        {
            var qty = ParseNumber(_quantity);
            var price = CurrencyInputBehavior.GetValue(_unitPrice);
            return qty * price;
        }
    }

    /// <summary>Formatted subtotal for display: "$37.000"</summary>
    public string SubtotalFormatted => FormatCurrency(Subtotal);

    private static decimal ParseNumber(string? text)
    {
        if (string.IsNullOrWhiteSpace(text)) return 0m;
        // Keep digits, periods (thousands sep or decimal), and commas (decimal sep)
        var cleaned = new string(text.Where(c => char.IsDigit(c) || c == ',' || c == '.').ToArray());
        // If it has periods as thousands separators but no comma, strip periods
        // If it has a comma, treat comma as decimal separator
        if (cleaned.Contains(','))
        {
            // e.g. "1.500,5" → "1500.5" or "1,5" → "1.5"
            cleaned = cleaned.Replace(".", "").Replace(",", ".");
        }
        else
        {
            // e.g. "1.500" (thousands) or "1.5" (decimal)
            // Count periods: if more than one, they're thousands separators
            var periodCount = cleaned.Count(c => c == '.');
            if (periodCount > 1)
                cleaned = cleaned.Replace(".", "");
            // If exactly one period and digits after are exactly 3, it's likely thousands
            else if (periodCount == 1)
            {
                var afterDot = cleaned[(cleaned.IndexOf('.') + 1)..];
                if (afterDot.Length == 3)
                    cleaned = cleaned.Replace(".", ""); // thousands separator
                // else keep it as decimal point
            }
        }
        return decimal.TryParse(cleaned, System.Globalization.NumberStyles.Any,
            System.Globalization.CultureInfo.InvariantCulture, out var v) ? v : 0m;
    }

    private static string FormatCurrency(decimal value)
        => $"${value:N0}".Replace(",", ".");

    private void OnPropertyChanged(string name)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
