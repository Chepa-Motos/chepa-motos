using ChepaMotos.Models;

namespace ChepaMotos.Services;

/// <summary>
/// Genera PDFs de factura tipo recibo (80mm térmica). La generación es
/// CPU-bound (QuestPDF) y puede tardar segundos para facturas con muchos
/// ítems, así que la API es async + cancelable para no bloquear el dispatcher.
/// </summary>
public interface IInvoicePdfService
{
    /// <summary>Genera el PDF en memoria y devuelve los bytes.</summary>
    Task<byte[]> GenerateReceiptAsync(Invoice invoice, CancellationToken ct = default);

    /// <summary>Genera y persiste a la ruta indicada.</summary>
    Task SaveReceiptAsync(Invoice invoice, string filePath, CancellationToken ct = default);
}
