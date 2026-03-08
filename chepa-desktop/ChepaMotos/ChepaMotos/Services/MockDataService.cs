using ChepaMotos.Models;

namespace ChepaMotos.Services;

/// <summary>
/// Simulates data "retrieved from the database" for offline development.
/// Methods mirror the future API service interface so swapping to real HTTP calls is straightforward.
/// All data lives in memory — resets on app restart.
/// </summary>
public static class MockDataService
{
    private static readonly List<Mechanic> _mechanics;
    private static readonly List<Vehicle> _vehicles;
    private static readonly List<Invoice> _invoices;
    private static readonly List<Liquidation> _liquidations;

    static MockDataService()
    {
        _mechanics = BuildMechanics();
        _vehicles = BuildVehicles();
        _invoices = BuildInvoices();
        _liquidations = BuildLiquidations();
    }

    // ── Mechanics ────────────────────────────────────────

    public static List<Mechanic> GetMechanics(bool? activeOnly = null)
    {
        if (activeOnly is bool active)
            return _mechanics.Where(m => m.IsActive == active).ToList();
        return _mechanics.ToList();
    }

    public static Mechanic? GetMechanicById(long id)
        => _mechanics.FirstOrDefault(m => m.Id == id);

    public static void UpdateMechanicStatus(long id, bool isActive)
    {
        var mechanic = _mechanics.FirstOrDefault(m => m.Id == id);
        if (mechanic is not null)
            mechanic.IsActive = isActive;
    }

    public static Mechanic AddMechanic(string name)
    {
        var mechanic = new Mechanic
        {
            Id = _mechanics.Max(m => m.Id) + 1,
            Name = name.Trim(),
            IsActive = true,
        };
        _mechanics.Add(mechanic);
        return mechanic;
    }

    // ── Vehicles ─────────────────────────────────────────

    public static Vehicle? GetVehicleByPlate(string plate)
        => _vehicles.FirstOrDefault(v => v.Plate.Equals(plate, StringComparison.OrdinalIgnoreCase));

    // ── Invoices ─────────────────────────────────────────

    /// <summary>
    /// Get invoices with optional filters. Mirrors GET /invoices query params.
    /// </summary>
    public static List<Invoice> GetInvoices(
        DateTime? date = null,
        string? type = null,
        long? mechanicId = null,
        bool? cancelled = null)
    {
        IEnumerable<Invoice> query = _invoices;

        if (date is DateTime d)
            query = query.Where(i => i.CreatedAt.Date == d.Date);

        if (type is not null)
            query = query.Where(i => i.InvoiceType.Equals(type, StringComparison.OrdinalIgnoreCase));

        if (mechanicId is long mid)
            query = query.Where(i => i.Mechanic?.Id == mid);

        if (cancelled is bool c)
            query = query.Where(i => i.IsCancelled == c);

        return query.OrderByDescending(i => i.CreatedAt).ToList();
    }

    public static Invoice? GetInvoiceById(long id)
        => _invoices.FirstOrDefault(i => i.Id == id);

    public static void CancelInvoice(long id)
    {
        var invoice = _invoices.FirstOrDefault(i => i.Id == id);
        if (invoice is not null)
            invoice.IsCancelled = true;
    }

    // ── Liquidations ─────────────────────────────────────

    public static List<Liquidation> GetLiquidations(long? mechanicId = null, DateTime? date = null)
    {
        IEnumerable<Liquidation> query = _liquidations;

        if (mechanicId is long mid)
            query = query.Where(l => l.Mechanic.Id == mid);

        if (date is DateTime d)
            query = query.Where(l => l.Date == d.ToString("yyyy-MM-dd"));

        return query.OrderByDescending(l => l.Date).ThenBy(l => l.Mechanic.Name).ToList();
    }

    // ── KPI helpers ──────────────────────────────────────

    public static (decimal totalBilled, decimal shopCut, decimal average, int invoiceCount,
                    int activeMechanics, int totalMechanics)
        GetTodayKpis()
    {
        var today = DateTime.Today;
        var todayInvoices = _invoices
            .Where(i => i.CreatedAt.Date == today && !i.IsCancelled)
            .ToList();

        var total = todayInvoices.Sum(i => i.TotalAmount);
        var serviceTotal = todayInvoices
            .Where(i => i.InvoiceType == "SERVICE")
            .Sum(i => i.TotalAmount);
        var shopCut = serviceTotal * 0.30m;
        var avg = todayInvoices.Count > 0 ? total / todayInvoices.Count : 0m;

        return (total, shopCut, avg, todayInvoices.Count,
                _mechanics.Count(m => m.IsActive), _mechanics.Count);
    }

    /// <summary>
    /// Returns today's active SERVICE invoice count per mechanic.
    /// </summary>
    public static Dictionary<long, (int count, decimal total)> GetTodayInvoiceCountByMechanic()
    {
        var today = DateTime.Today;
        return _invoices
            .Where(i => i.CreatedAt.Date == today && !i.IsCancelled && i.InvoiceType == "SERVICE" && i.Mechanic is not null)
            .GroupBy(i => i.Mechanic!.Id)
            .ToDictionary(g => g.Key, g => (g.Count(), g.Sum(i => i.TotalAmount)));
    }

    // ── Currency helper ──────────────────────────────────

    public static string FormatCurrency(decimal value)
        => $"${value:N0}".Replace(",", ".");

    // ══════════════════════════════════════════════════════
    // SEED DATA
    // ══════════════════════════════════════════════════════

    private static List<Mechanic> BuildMechanics() =>
    [
        new() { Id = 1, Name = "Jose Garcia", IsActive = true },
        new() { Id = 2, Name = "Carlos Martínez", IsActive = true },
        new() { Id = 3, Name = "Pedro López", IsActive = true },
        new() { Id = 4, Name = "Miguel Ángel", IsActive = true },
        new() { Id = 5, Name = "Andrés Ríos", IsActive = false },
        new() { Id = 6, Name = "Luis Hernández", IsActive = false },
    ];

    private static List<Vehicle> BuildVehicles() =>
    [
        new() { Id = 1, Plate = "BXR42H", Model = "Boxer 150 2021" },
        new() { Id = 2, Plate = "KDP93F", Model = "Pulsar NS 200 2023" },
        new() { Id = 3, Plate = "MNQ17E", Model = "NKD 125 2020" },
        new() { Id = 4, Plate = "TPL28C", Model = "Crypton FI 115 2022" },
        new() { Id = 5, Plate = "GHL54A", Model = "FZ 250 2024" },
        new() { Id = 6, Plate = "RMS61D", Model = "AKT TT 150 2022" },
        new() { Id = 7, Plate = "WXY99B", Model = "Gixxer 250 2023" },
        new() { Id = 8, Plate = "JNP45F", Model = "XTZ 125 2021" },
    ];

    private static List<Invoice> BuildInvoices()
    {
        var today = DateTime.Today;
        var yesterday = today.AddDays(-1);
        var twoDaysAgo = today.AddDays(-2);

        var m1 = new Mechanic { Id = 1, Name = "Jose Garcia", IsActive = true };
        var m2 = new Mechanic { Id = 2, Name = "Carlos Martínez", IsActive = true };
        var m3 = new Mechanic { Id = 3, Name = "Pedro López", IsActive = true };
        var m4 = new Mechanic { Id = 4, Name = "Miguel Ángel", IsActive = true };

        var v1 = new Vehicle { Id = 1, Plate = "BXR42H", Model = "Boxer 150 2021" };
        var v2 = new Vehicle { Id = 2, Plate = "KDP93F", Model = "Pulsar NS 200 2023" };
        var v3 = new Vehicle { Id = 3, Plate = "MNQ17E", Model = "NKD 125 2020" };
        var v4 = new Vehicle { Id = 4, Plate = "TPL28C", Model = "Crypton FI 115 2022" };
        var v5 = new Vehicle { Id = 5, Plate = "GHL54A", Model = "FZ 250 2024" };
        var v6 = new Vehicle { Id = 6, Plate = "RMS61D", Model = "AKT TT 150 2022" };
        var v7 = new Vehicle { Id = 7, Plate = "WXY99B", Model = "Gixxer 250 2023" };
        var v8 = new Vehicle { Id = 8, Plate = "JNP45F", Model = "XTZ 125 2021" };

        return
        [
            // ── TODAY ────────────────────────────────────

            new()
            {
                Id = 14, InvoiceType = "SERVICE", Mechanic = m1, Vehicle = v1,
                CreatedAt = today.AddHours(9).AddMinutes(45),
                LaborAmount = 65000m, TotalAmount = 209100m,
                Items =
                [
                    new() { Id = 51, Description = "Tornillo Leva", Quantity = 1, UnitPrice = 3900m, Subtotal = 3900m },
                    new() { Id = 52, Description = "Pastillas de freno delantero", Quantity = 2, UnitPrice = 18500m, Subtotal = 37000m },
                    new() { Id = 53, Description = "Aceite Motor 4T 20W-50", Quantity = 1, UnitPrice = 28200m, Subtotal = 28200m },
                    new() { Id = 54, Description = "Filtro de aceite", Quantity = 1, UnitPrice = 15000m, Subtotal = 15000m },
                ],
            },
            new()
            {
                Id = 13, InvoiceType = "DELIVERY", BuyerName = "Talleres La 80",
                CreatedAt = today.AddHours(9).AddMinutes(22),
                TotalAmount = 37000m,
                Items =
                [
                    new() { Id = 49, Description = "Suzuki 150 Palanca de Freno", Quantity = 2, UnitPrice = 18500m, Subtotal = 37000m },
                ],
            },
            new()
            {
                Id = 12, InvoiceType = "SERVICE", Mechanic = m2, Vehicle = v2,
                CreatedAt = today.AddHours(8).AddMinutes(55),
                LaborAmount = 45000m, TotalAmount = 156400m,
                Items =
                [
                    new() { Id = 46, Description = "Kit de arrastre completo", Quantity = 1, UnitPrice = 85000m, Subtotal = 85000m },
                    new() { Id = 47, Description = "Lubricante cadena", Quantity = 1, UnitPrice = 12400m, Subtotal = 12400m },
                    new() { Id = 48, Description = "Tensor de cadena", Quantity = 1, UnitPrice = 14000m, Subtotal = 14000m },
                ],
            },
            new()
            {
                Id = 11, InvoiceType = "SERVICE", Mechanic = m3, Vehicle = v3,
                CreatedAt = today.AddHours(8).AddMinutes(30),
                LaborAmount = 30000m, TotalAmount = 89500m,
                Items =
                [
                    new() { Id = 44, Description = "Bujía NGK CR7HSA", Quantity = 1, UnitPrice = 12500m, Subtotal = 12500m },
                    new() { Id = 45, Description = "Cable de embrague", Quantity = 1, UnitPrice = 47000m, Subtotal = 47000m },
                ],
            },
            new()
            {
                Id = 10, InvoiceType = "DELIVERY", BuyerName = "Motos del Sur",
                CreatedAt = today.AddHours(8).AddMinutes(10),
                TotalAmount = 54200m,
                Items =
                [
                    new() { Id = 42, Description = "Espejo retrovisor derecho universal", Quantity = 2, UnitPrice = 15600m, Subtotal = 31200m },
                    new() { Id = 43, Description = "Manigueta derecha", Quantity = 1, UnitPrice = 23000m, Subtotal = 23000m },
                ],
            },
            new()
            {
                Id = 9, InvoiceType = "SERVICE", Mechanic = m4, Vehicle = v4,
                CreatedAt = today.AddHours(7).AddMinutes(50),
                LaborAmount = 55000m, TotalAmount = 178300m,
                Items =
                [
                    new() { Id = 39, Description = "Llanta trasera 275-17", Quantity = 1, UnitPrice = 62000m, Subtotal = 62000m },
                    new() { Id = 40, Description = "Rin trasero", Quantity = 1, UnitPrice = 45000m, Subtotal = 45000m },
                    new() { Id = 41, Description = "Mano de montaje", Quantity = 1, UnitPrice = 16300m, Subtotal = 16300m },
                ],
            },
            new()
            {
                Id = 8, InvoiceType = "SERVICE", Mechanic = m1, Vehicle = v5,
                CreatedAt = today.AddHours(7).AddMinutes(15),
                LaborAmount = 80000m, TotalAmount = 322500m,
                Items =
                [
                    new() { Id = 36, Description = "Pastillas de freno trasero", Quantity = 1, UnitPrice = 22500m, Subtotal = 22500m },
                    new() { Id = 37, Description = "Disco de freno delantero", Quantity = 1, UnitPrice = 135000m, Subtotal = 135000m },
                    new() { Id = 38, Description = "Líquido de frenos DOT 4", Quantity = 2, UnitPrice = 42500m, Subtotal = 85000m },
                ],
            },

            // ── TODAY — cancelled ────────────────────────

            new()
            {
                Id = 7, InvoiceType = "SERVICE", Mechanic = m2, Vehicle = v6,
                CreatedAt = today.AddHours(7).AddMinutes(5),
                LaborAmount = 25000m, TotalAmount = 67500m,
                IsCancelled = true,
                Items =
                [
                    new() { Id = 35, Description = "Guaya de freno trasero", Quantity = 1, UnitPrice = 42500m, Subtotal = 42500m },
                ],
            },

            // ── YESTERDAY ────────────────────────────────

            new()
            {
                Id = 6, InvoiceType = "SERVICE", Mechanic = m1, Vehicle = v6,
                CreatedAt = yesterday.AddHours(16).AddMinutes(30),
                LaborAmount = 40000m, TotalAmount = 152000m,
                Items =
                [
                    new() { Id = 30, Description = "Cambio de aceite", Quantity = 1, UnitPrice = 28000m, Subtotal = 28000m },
                    new() { Id = 31, Description = "Filtro de aceite", Quantity = 1, UnitPrice = 15000m, Subtotal = 15000m },
                    new() { Id = 32, Description = "Bujía NGK", Quantity = 1, UnitPrice = 12000m, Subtotal = 12000m },
                    new() { Id = 33, Description = "Revisión general", Quantity = 1, UnitPrice = 57000m, Subtotal = 57000m },
                ],
            },
            new()
            {
                Id = 5, InvoiceType = "SERVICE", Mechanic = m1, Vehicle = v7,
                CreatedAt = yesterday.AddHours(14).AddMinutes(20),
                LaborAmount = 50000m, TotalAmount = 198000m,
                Items =
                [
                    new() { Id = 27, Description = "Llanta delantera 300-17", Quantity = 1, UnitPrice = 72000m, Subtotal = 72000m },
                    new() { Id = 28, Description = "Cámara 300-17", Quantity = 1, UnitPrice = 18000m, Subtotal = 18000m },
                    new() { Id = 29, Description = "Balanceo", Quantity = 1, UnitPrice = 58000m, Subtotal = 58000m },
                ],
            },
            new()
            {
                Id = 4, InvoiceType = "DELIVERY", BuyerName = "Repuestos El Paisa",
                CreatedAt = yesterday.AddHours(12).AddMinutes(45),
                TotalAmount = 124800m,
                Items =
                [
                    new() { Id = 24, Description = "Cadena 428H 120L", Quantity = 3, UnitPrice = 32000m, Subtotal = 96000m },
                    new() { Id = 25, Description = "Candado cadena", Quantity = 3, UnitPrice = 4800m, Subtotal = 14400m },
                    new() { Id = 26, Description = "Piñón conducido 42T", Quantity = 1, UnitPrice = 14400m, Subtotal = 14400m },
                ],
            },
            new()
            {
                Id = 3, InvoiceType = "SERVICE", Mechanic = m2, Vehicle = v8,
                CreatedAt = yesterday.AddHours(10).AddMinutes(15),
                LaborAmount = 35000m, TotalAmount = 128500m,
                Items =
                [
                    new() { Id = 21, Description = "Cable de acelerador", Quantity = 1, UnitPrice = 38500m, Subtotal = 38500m },
                    new() { Id = 22, Description = "Manigueta izquierda", Quantity = 1, UnitPrice = 25000m, Subtotal = 25000m },
                    new() { Id = 23, Description = "Empaque de motor", Quantity = 1, UnitPrice = 30000m, Subtotal = 30000m },
                ],
            },
            new()
            {
                Id = 2, InvoiceType = "SERVICE", Mechanic = m3, Vehicle = v4,
                CreatedAt = yesterday.AddHours(9).AddMinutes(30),
                LaborAmount = 45000m, TotalAmount = 167000m,
                Items =
                [
                    new() { Id = 18, Description = "Rodamiento rueda trasera", Quantity = 2, UnitPrice = 28000m, Subtotal = 56000m },
                    new() { Id = 19, Description = "Retenedor rueda", Quantity = 2, UnitPrice = 8000m, Subtotal = 16000m },
                    new() { Id = 20, Description = "Espaciador eje", Quantity = 1, UnitPrice = 50000m, Subtotal = 50000m },
                ],
            },

            // ── TWO DAYS AGO ─────────────────────────────

            new()
            {
                Id = 1, InvoiceType = "SERVICE", Mechanic = m4, Vehicle = v1,
                CreatedAt = twoDaysAgo.AddHours(15).AddMinutes(10),
                LaborAmount = 60000m, TotalAmount = 245000m,
                Items =
                [
                    new() { Id = 15, Description = "Kit de embrague completo", Quantity = 1, UnitPrice = 95000m, Subtotal = 95000m },
                    new() { Id = 16, Description = "Aceite transmisión", Quantity = 1, UnitPrice = 35000m, Subtotal = 35000m },
                    new() { Id = 17, Description = "Resorte embrague", Quantity = 3, UnitPrice = 18333m, Subtotal = 55000m },
                ],
            },
        ];
    }

    private static List<Liquidation> BuildLiquidations()
    {
        var yesterday = DateTime.Today.AddDays(-1).ToString("yyyy-MM-dd");
        var twoDaysAgo = DateTime.Today.AddDays(-2).ToString("yyyy-MM-dd");

        var m1 = new Mechanic { Id = 1, Name = "Jose Garcia", IsActive = true };
        var m2 = new Mechanic { Id = 2, Name = "Carlos Martínez", IsActive = true };
        var m3 = new Mechanic { Id = 3, Name = "Pedro López", IsActive = true };
        var m4 = new Mechanic { Id = 4, Name = "Miguel Ángel", IsActive = true };

        // Yesterday: Jose had 2 service invoices (IDs 5,6), Carlos had 1 (ID 3), Pedro had 1 (ID 2)
        return
        [
            new() { Id = 1, Mechanic = m1, Date = yesterday, InvoiceCount = 2, TotalRevenue = 350000m, MechanicShare = 245000m, ShopShare = 105000m },
            new() { Id = 2, Mechanic = m2, Date = yesterday, InvoiceCount = 1, TotalRevenue = 128500m, MechanicShare = 89950m, ShopShare = 38550m },
            new() { Id = 3, Mechanic = m3, Date = yesterday, InvoiceCount = 1, TotalRevenue = 167000m, MechanicShare = 116900m, ShopShare = 50100m },
            new() { Id = 4, Mechanic = m4, Date = twoDaysAgo, InvoiceCount = 1, TotalRevenue = 245000m, MechanicShare = 171500m, ShopShare = 73500m },
        ];
    }
}
