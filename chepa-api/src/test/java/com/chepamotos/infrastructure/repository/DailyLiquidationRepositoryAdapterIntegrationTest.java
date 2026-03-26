package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.model.Mechanic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DailyLiquidationRepositoryAdapter.class, MechanicRepositoryAdapter.class})
class DailyLiquidationRepositoryAdapterIntegrationTest {

    @Autowired
    private DailyLiquidationRepositoryAdapter dailyLiquidationRepositoryAdapter;

    @Autowired
    private MechanicRepositoryAdapter mechanicRepositoryAdapter;

    @Test
    void save_persistsLiquidationAndReturnsGeneratedId() {
        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_save"));
        LocalDate date = LocalDate.of(2099, 1, 1);

        DailyLiquidation toSave = DailyLiquidation.create(mechanic, date, new BigDecimal("100000.00"), 2);
        DailyLiquidation saved = dailyLiquidationRepositoryAdapter.save(toSave);

        assertNotNull(saved.id());
        assertEquals(mechanic.id(), saved.mechanic().id());
        assertEquals(date, saved.date());
        assertEquals(new BigDecimal("100000.00"), saved.totalRevenue());
        assertEquals(new BigDecimal("70000.00"), saved.mechanicShare());
        assertEquals(new BigDecimal("30000.00"), saved.shopShare());
        assertEquals(2, saved.invoiceCount());
    }

    @Test
    void existsByMechanicAndDate_returnsTrueWhenLiquidationExists() {
        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_exists"));
        LocalDate date = LocalDate.of(2099, 1, 2);

        dailyLiquidationRepositoryAdapter.save(
                DailyLiquidation.create(mechanic, date, new BigDecimal("50000.00"), 1)
        );

        boolean exists = dailyLiquidationRepositoryAdapter.existsByMechanicAndDate(mechanic.id(), date);

        assertTrue(exists);
    }

    @Test
    void existsByMechanicAndDate_returnsFalseWhenLiquidationDoesNotExist() {
        Mechanic mechanic = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_not_exists"));
        LocalDate date = LocalDate.of(2099, 1, 22);

        boolean exists = dailyLiquidationRepositoryAdapter.existsByMechanicAndDate(mechanic.id(), date);

        assertFalse(exists);
    }

    @Test
    void findAll_appliesMechanicAndDateFilters() {
        LocalDate targetDate = LocalDate.of(2099, 1, 3);
        LocalDate otherDate = LocalDate.of(2099, 1, 4);

        Mechanic mechanicA = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_filter_A"));
        Mechanic mechanicB = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_filter_B"));

        dailyLiquidationRepositoryAdapter.save(
                DailyLiquidation.create(mechanicA, targetDate, new BigDecimal("80000.00"), 2)
        );
        dailyLiquidationRepositoryAdapter.save(
                DailyLiquidation.create(mechanicA, otherDate, new BigDecimal("90000.00"), 2)
        );
        dailyLiquidationRepositoryAdapter.save(
                DailyLiquidation.create(mechanicB, targetDate, new BigDecimal("60000.00"), 1)
        );

        List<DailyLiquidation> filtered = dailyLiquidationRepositoryAdapter.findAll(mechanicA.id(), targetDate);

        assertEquals(1, filtered.size());
        assertEquals(mechanicA.id(), filtered.get(0).mechanic().id());
        assertEquals(targetDate, filtered.get(0).date());
    }

        @Test
        void findAll_whenFiltersAreNull_returnsAllRows() {
        LocalDate targetDate = LocalDate.of(2099, 1, 5);
        LocalDate otherDate = LocalDate.of(2099, 1, 6);

        Mechanic mechanicA = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_null_A"));
        Mechanic mechanicB = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_null_B"));

        dailyLiquidationRepositoryAdapter.save(
            DailyLiquidation.create(mechanicA, targetDate, new BigDecimal("70000.00"), 2)
        );
        dailyLiquidationRepositoryAdapter.save(
            DailyLiquidation.create(mechanicB, otherDate, new BigDecimal("40000.00"), 1)
        );

        List<DailyLiquidation> all = dailyLiquidationRepositoryAdapter.findAll(null, null);

        assertTrue(all.stream().anyMatch(liq -> liq.mechanic().id().equals(mechanicA.id()) && liq.date().equals(targetDate)));
        assertTrue(all.stream().anyMatch(liq -> liq.mechanic().id().equals(mechanicB.id()) && liq.date().equals(otherDate)));
        }

        @Test
        void findAll_whenOnlyDateIsProvided_filtersByDateOnly() {
        LocalDate targetDate = LocalDate.of(2099, 1, 7);
        LocalDate otherDate = LocalDate.of(2099, 1, 8);

        Mechanic mechanicA = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_date_A"));
        Mechanic mechanicB = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_date_B"));

        dailyLiquidationRepositoryAdapter.save(
            DailyLiquidation.create(mechanicA, targetDate, new BigDecimal("50000.00"), 1)
        );
        dailyLiquidationRepositoryAdapter.save(
            DailyLiquidation.create(mechanicB, otherDate, new BigDecimal("80000.00"), 2)
        );

        List<DailyLiquidation> byDate = dailyLiquidationRepositoryAdapter.findAll(null, targetDate);

        assertEquals(1, byDate.size());
        assertEquals(targetDate, byDate.get(0).date());
        assertEquals(mechanicA.id(), byDate.get(0).mechanic().id());
        }

        @Test
        void findAll_whenOnlyMechanicIsProvided_filtersByMechanicOnly() {
        LocalDate dateA = LocalDate.of(2099, 1, 9);
        LocalDate dateB = LocalDate.of(2099, 1, 10);

        Mechanic mechanicA = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_mech_A"));
        Mechanic mechanicB = mechanicRepositoryAdapter.save(Mechanic.createNew("Tester_liq_mech_B"));

        dailyLiquidationRepositoryAdapter.save(
            DailyLiquidation.create(mechanicA, dateA, new BigDecimal("120000.00"), 3)
        );
        dailyLiquidationRepositoryAdapter.save(
            DailyLiquidation.create(mechanicB, dateB, new BigDecimal("30000.00"), 1)
        );

        List<DailyLiquidation> byMechanic = dailyLiquidationRepositoryAdapter.findAll(mechanicA.id(), null);

        assertEquals(1, byMechanic.size());
        assertEquals(mechanicA.id(), byMechanic.get(0).mechanic().id());
        assertEquals(dateA, byMechanic.get(0).date());
        }
}
