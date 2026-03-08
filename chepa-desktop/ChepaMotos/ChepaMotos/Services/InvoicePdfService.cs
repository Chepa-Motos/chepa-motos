using ChepaMotos.Models;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;
using QColors = QuestPDF.Helpers.Colors;

namespace ChepaMotos.Services;

/// <summary>
/// Generates a receipt-style PDF for an invoice, sized for 80mm thermal printers.
/// Uses QuestPDF fluent API. The same PDF is used for both printing and exporting.
/// </summary>
public static class InvoicePdfService
{
    // 80mm thermal: ~72mm printable = ~204pt
    private const float PageWidthPt = 204f;
    private const float SideMargin = 6f;

    static InvoicePdfService()
    {
        QuestPDF.Settings.License = LicenseType.Community;
    }

    /// <summary>
    /// Generates the receipt PDF in memory and returns the raw bytes.
    /// </summary>
    public static byte[] GenerateReceiptPdf(Invoice invoice)
    {
        var document = Document.Create(container =>
        {
            container.Page(page =>
            {
                page.ContinuousSize(PageWidthPt);
                page.MarginHorizontal(SideMargin, Unit.Point);
                page.MarginVertical(8, Unit.Point);
                page.DefaultTextStyle(x => x.FontSize(8).FontFamily("IBM Plex Sans"));

                page.Content().Column(col =>
                {
                    col.Spacing(4);

                    // ── Shop header ──────────────────────────
                    col.Item().AlignCenter().Text("ALMACEN Y TALLER")
                        .FontFamily("IBM Plex Mono").Bold().FontSize(9);
                    col.Item().AlignCenter().Text("CHEPA MOTOS")
                        .FontFamily("IBM Plex Mono").Bold().FontSize(9);
                    col.Item().AlignCenter().Text("Nit. 1.036.621.093-1")
                        .FontFamily("IBM Plex Mono").FontSize(7);
                    col.Item().AlignCenter().Text("Calle 2A No. 78A-29 Belén Rincón")
                        .FontFamily("IBM Plex Mono").FontSize(7);
                    col.Item().AlignCenter().Text("Tel. (604) 6030702")
                        .FontFamily("IBM Plex Mono").FontSize(7);

                    // Divider
                    col.Item().PaddingVertical(3).LineHorizontal(0.5f).LineColor(QColors.Grey.Medium);

                    // ── Invoice meta ─────────────────────────
                    var isService = invoice.InvoiceType == "SERVICE";
                    var typeLabel = isService ? "FACTURA DE SERVICIO" : "FACTURA DE VENTA";
                    col.Item().AlignCenter().Text(typeLabel)
                        .FontFamily("IBM Plex Mono").SemiBold().FontSize(9);

                    col.Item().AlignCenter().Text($"# {invoice.Id:D3}")
                        .FontFamily("IBM Plex Mono").SemiBold().FontSize(10);

                    if (invoice.IsCancelled)
                    {
                        col.Item().AlignCenter().Text("*** ANULADA ***")
                            .FontFamily("IBM Plex Mono").Bold().FontSize(10).FontColor(QColors.Red.Medium);
                    }

                    col.Item().AlignCenter().Text(invoice.CreatedAt.ToString("dd/MM/yyyy  HH:mm"))
                        .FontFamily("IBM Plex Mono").FontSize(7).FontColor(QColors.Grey.Darken1);

                    // Divider
                    col.Item().PaddingVertical(3).LineHorizontal(0.5f).LineColor(QColors.Grey.Medium);

                    // ── Fields ────────────────────────────────
                    if (isService)
                    {
                        AddField(col, "Mecánico", invoice.Mechanic?.Name ?? "—");
                        AddField(col, "Placa", invoice.Vehicle?.Plate ?? "—");
                        AddField(col, "Modelo", invoice.Vehicle?.Model ?? "—");
                    }
                    else
                    {
                        AddField(col, "Comprador", invoice.BuyerName ?? "—");
                    }

                    // Divider
                    col.Item().PaddingVertical(3).LineHorizontal(0.5f).LineColor(QColors.Grey.Medium);

                    // ── Items table header ────────────────────
                    col.Item().Row(row =>
                    {
                        row.ConstantItem(22).Text("Cant").FontFamily("IBM Plex Mono").FontSize(7).Bold();
                        row.RelativeItem().Text("Descripción").FontSize(7).Bold();
                        row.ConstantItem(50).AlignRight().Text("Vr. Unit").FontFamily("IBM Plex Mono").FontSize(7).Bold();
                        row.ConstantItem(50).AlignRight().Text("Subtotal").FontFamily("IBM Plex Mono").FontSize(7).Bold();
                    });

                    col.Item().LineHorizontal(0.3f).LineColor(QColors.Grey.Medium);

                    // ── Item rows ─────────────────────────────
                    foreach (var item in invoice.Items)
                    {
                        col.Item().Row(row =>
                        {
                            var qtyText = item.Quantity % 1 == 0 ? $"{(int)item.Quantity}" : $"{item.Quantity}";
                            row.ConstantItem(22).Text(qtyText).FontFamily("IBM Plex Mono").FontSize(7);
                            row.RelativeItem().Text(item.Description).FontSize(7);
                            row.ConstantItem(50).AlignRight().Text(Fmt(item.UnitPrice)).FontFamily("IBM Plex Mono").FontSize(7);
                            row.ConstantItem(50).AlignRight().Text(Fmt(item.Subtotal)).FontFamily("IBM Plex Mono").FontSize(7);
                        });
                    }

                    // Divider
                    col.Item().PaddingVertical(3).LineHorizontal(0.5f).LineColor(QColors.Grey.Medium);

                    // ── Labor (SERVICE only) ──────────────────
                    if (isService && invoice.LaborAmount > 0)
                    {
                        col.Item().Row(row =>
                        {
                            row.RelativeItem().Text("Mano de obra").SemiBold().FontSize(8);
                            row.ConstantItem(65).AlignRight().Text(Fmt(invoice.LaborAmount))
                                .FontFamily("IBM Plex Mono").SemiBold().FontSize(8);
                        });
                        col.Item().PaddingVertical(2).LineHorizontal(0.3f).LineColor(QColors.Grey.Medium);
                    }

                    // ── Total ─────────────────────────────────
                    col.Item().PaddingVertical(2).Row(row =>
                    {
                        row.RelativeItem().Text("TOTAL").FontFamily("IBM Plex Mono").Bold().FontSize(10);
                        row.ConstantItem(75).AlignRight().Text(Fmt(invoice.TotalAmount))
                            .FontFamily("IBM Plex Mono").Bold().FontSize(10);
                    });

                    // ── Footer ────────────────────────────────
                    col.Item().PaddingVertical(4).LineHorizontal(0.5f).LineColor(QColors.Grey.Medium);
                    col.Item().AlignCenter().Text("¡Gracias por su compra!")
                        .FontSize(7).Italic();
                    col.Item().AlignCenter().Text("Chepa Motos — Medellín, Colombia")
                        .FontFamily("IBM Plex Mono").FontSize(6).FontColor(QColors.Grey.Darken1);
                });
            });
        });

        return document.GeneratePdf();
    }

    /// <summary>
    /// Save the receipt PDF to the specified file path.
    /// </summary>
    public static void SaveReceiptPdf(Invoice invoice, string filePath)
    {
        var bytes = GenerateReceiptPdf(invoice);
        File.WriteAllBytes(filePath, bytes);
    }

    // ── Helpers ──────────────────────────────────────────

    private static void AddField(ColumnDescriptor col, string label, string value)
    {
        col.Item().Row(row =>
        {
            row.ConstantItem(55).Text(label + ":").FontSize(7).Bold();
            row.RelativeItem().Text(value).FontFamily("IBM Plex Mono").FontSize(7);
        });
    }

    private static string Fmt(decimal value)
        => $"${value:N0}".Replace(",", ".");
}
