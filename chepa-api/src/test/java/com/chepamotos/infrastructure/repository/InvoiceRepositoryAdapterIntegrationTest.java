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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    void save_whenInvoiceCancelled_persistsCancelledFlag() {
        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_inv_cancel"));
        Vehicle vehicle = vehicleRepositoryAdapter.save(Vehicle.createNew("TSTINV4D", "Test Model Cancel"));
        List<InvoiceItem> items = List.of(InvoiceItem.createNew("Cancel item", BigDecimal.ONE, new BigDecimal("12000.00")));

        Invoice saved = invoiceRepositoryAdapter.save(Invoice.createService(mechanic, vehicle, new BigDecimal("10000.00"), items));
        Invoice cancelled = saved.cancel();

        Invoice persisted = invoiceRepositoryAdapter.save(cancelled);

        assertTrue(persisted.cancelled());
        assertEquals(saved.id(), persisted.id());
    }

        @Test
        void sumAndCountActiveServiceByMechanicAndDate_excludesCancelledAndDeliveryAndOtherDates() {
        LocalDate date = LocalDate.of(2099, 2, 1);
        LocalDate otherDate = LocalDate.of(2099, 2, 2);

        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_inv_agg_main"));
        Vehicle vehicle = vehicleRepositoryAdapter.save(Vehicle.createNew("TSTAGG1", "Agg Model Main"));

        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.SERVICE,
            mechanic,
            vehicle,
            null,
            LocalDateTime.of(2099, 2, 1, 10, 0),
            new BigDecimal("100.00"),
            false,
            List.of(InvoiceItem.createNew("Item 1", BigDecimal.ONE, new BigDecimal("10.00")))
        ));
        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.SERVICE,
            mechanic,
            vehicle,
            null,
            LocalDateTime.of(2099, 2, 1, 12, 0),
            new BigDecimal("50.00"),
            false,
            List.of(InvoiceItem.createNew("Item 2", BigDecimal.ONE, new BigDecimal("10.00")))
        ));

        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.SERVICE,
            mechanic,
            vehicle,
            null,
            LocalDateTime.of(2099, 2, 1, 13, 0),
            new BigDecimal("999.00"),
            true,
            List.of(InvoiceItem.createNew("Cancelled", BigDecimal.ONE, new BigDecimal("10.00")))
        ));

        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.SERVICE,
            mechanic,
            vehicle,
            null,
            LocalDateTime.of(2099, 2, 2, 9, 0),
            new BigDecimal("777.00"),
            false,
            List.of(InvoiceItem.createNew("Other date", BigDecimal.ONE, new BigDecimal("10.00")))
        ));

        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.DELIVERY,
            null,
            null,
            "Buyer Agg",
            LocalDateTime.of(2099, 2, 1, 15, 0),
            BigDecimal.ZERO,
            false,
            List.of(InvoiceItem.createNew("Delivery", BigDecimal.ONE, new BigDecimal("10.00")))
        ));

        BigDecimal total = invoiceRepositoryAdapter.sumActiveServiceLaborByMechanicAndDate(mechanic.id(), date);
        int count = invoiceRepositoryAdapter.countActiveServiceInvoicesByMechanicAndDate(mechanic.id(), date);

        assertEquals(new BigDecimal("150.00"), total);
        assertEquals(2, count);

        BigDecimal otherTotal = invoiceRepositoryAdapter.sumActiveServiceLaborByMechanicAndDate(mechanic.id(), otherDate);
        int otherCount = invoiceRepositoryAdapter.countActiveServiceInvoicesByMechanicAndDate(mechanic.id(), otherDate);

        assertEquals(new BigDecimal("777.00"), otherTotal);
        assertEquals(1, otherCount);
        }

        @Test
        void findActiveMechanicIdsWithActiveServiceInvoicesByDate_returnsOnlyActiveMechanicsWithServiceInvoices() {
        LocalDate date = LocalDate.of(2099, 3, 1);

        Mechanic activeMechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_inv_agg_active"));
        Mechanic inactiveMechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_inv_agg_inactive"));
        inactiveMechanic = mechanicRepositoryAdapter.save(inactiveMechanic.withStatus(false));

        Vehicle vehicle1 = vehicleRepositoryAdapter.save(Vehicle.createNew("TSTAGG2", "Agg Model Active"));
        Vehicle vehicle2 = vehicleRepositoryAdapter.save(Vehicle.createNew("TSTAGG3", "Agg Model Inactive"));

        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.SERVICE,
            activeMechanic,
            vehicle1,
            null,
            LocalDateTime.of(2099, 3, 1, 10, 0),
            new BigDecimal("80.00"),
            false,
            List.of(InvoiceItem.createNew("Active service", BigDecimal.ONE, new BigDecimal("10.00")))
        ));

        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.SERVICE,
            inactiveMechanic,
            vehicle2,
            null,
            LocalDateTime.of(2099, 3, 1, 11, 0),
            new BigDecimal("90.00"),
            false,
            List.of(InvoiceItem.createNew("Inactive service", BigDecimal.ONE, new BigDecimal("10.00")))
        ));

        invoiceRepositoryAdapter.save(Invoice.restore(
            null,
            InvoiceType.DELIVERY,
            null,
            null,
            "Buyer",
            LocalDateTime.of(2099, 3, 1, 12, 0),
            BigDecimal.ZERO,
            false,
            List.of(InvoiceItem.createNew("Delivery", BigDecimal.ONE, new BigDecimal("10.00")))
        ));

        List<Long> mechanicIds = invoiceRepositoryAdapter.findActiveMechanicIdsWithActiveServiceInvoicesByDate(date);

        assertEquals(1, mechanicIds.size());
        assertEquals(activeMechanic.id(), mechanicIds.get(0));
        }
}
