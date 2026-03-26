package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InvoiceRepositoryAdapter.class, MechanicRepositoryAdapter.class, VehicleRepositoryAdapter.class})
class InvoiceRepositoryAdapterIntegrationTest {

    @Autowired
    private InvoiceRepositoryAdapter invoiceRepositoryAdapter;

    @Autowired
    private MechanicRepositoryAdapter mechanicRepositoryAdapter;

    @Autowired
    private VehicleRepositoryAdapter vehicleRepositoryAdapter;

    @Test
    void save_serviceInvoice_persistsWithItemsAndReturnsGeneratedIds() {
        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_inv_save"));
        Vehicle vehicle = vehicleRepositoryAdapter.save(Vehicle.createNew("TSTINV1A", "Test Model Save"));

        List<InvoiceItem> items = List.of(InvoiceItem.createNew("Freno delantero", BigDecimal.ONE, new BigDecimal("36900.00")));
        Invoice toSave = Invoice.createService(mechanic, vehicle, new BigDecimal("45000.00"), items);

        Invoice saved = invoiceRepositoryAdapter.save(toSave);

        assertNotNull(saved.id());
        assertEquals(InvoiceType.SERVICE, saved.type());
        assertEquals(mechanic.id(), saved.mechanic().id());
        assertEquals(vehicle.id(), saved.vehicle().id());
        assertEquals(1, saved.items().size());
        assertNotNull(saved.items().get(0).id());
        assertEquals("Freno delantero", saved.items().get(0).description());
    }

    @Test
    void save_deliveryInvoice_persistsWithBuyerName() {
        List<InvoiceItem> items = List.of(InvoiceItem.createNew("Boxer 150 Palanca de freno", new BigDecimal("2"), new BigDecimal("18500.00")));
        Invoice toSave = Invoice.createDelivery("Talleres La 80", items);

        Invoice saved = invoiceRepositoryAdapter.save(toSave);

        assertNotNull(saved.id());
        assertEquals(InvoiceType.DELIVERY, saved.type());
        assertEquals("Talleres La 80", saved.buyerName());
        assertEquals(1, saved.items().size());
    }

    @Test
    void findById_returnsSavedInvoice() {
        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_inv_find"));
        Vehicle vehicle = vehicleRepositoryAdapter.save(Vehicle.createNew("TSTINV2B", "Test Model Find"));

        List<InvoiceItem> items = List.of(InvoiceItem.createNew("Bujía", BigDecimal.ONE, new BigDecimal("26000.00")));
        Invoice saved = invoiceRepositoryAdapter.save(Invoice.createService(mechanic, vehicle, new BigDecimal("30000.00"), items));

        var result = invoiceRepositoryAdapter.findById(saved.id());

        assertTrue(result.isPresent());
        assertEquals(saved.id(), result.get().id());
        assertEquals(InvoiceType.SERVICE, result.get().type());
        assertEquals("Bujía", result.get().items().get(0).description());
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        var result = invoiceRepositoryAdapter.findById(Long.MAX_VALUE);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_countIncreasesAfterSave() {
        int baseline = invoiceRepositoryAdapter.findAll().size();

        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_inv_all"));
        Vehicle vehicle = vehicleRepositoryAdapter.save(Vehicle.createNew("TSTINV3C", "Test Model All"));
        List<InvoiceItem> items = List.of(InvoiceItem.createNew("Test item", BigDecimal.ONE, new BigDecimal("10000.00")));
        invoiceRepositoryAdapter.save(Invoice.createService(mechanic, vehicle, new BigDecimal("10000.00"), items));

        List<Invoice> all = invoiceRepositoryAdapter.findAll();
        assertEquals(baseline + 1, all.size());
    }
}
