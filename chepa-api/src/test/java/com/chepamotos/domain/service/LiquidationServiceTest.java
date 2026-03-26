package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.LiquidationAlreadyExistsException;
import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.DailyLiquidationRepository;
import com.chepamotos.domain.port.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiquidationServiceTest {

    @Mock
    private DailyLiquidationRepository dailyLiquidationRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MechanicService mechanicService;

    @InjectMocks
    private LiquidationService liquidationService;

    @Test
    void list_delegatesToRepository() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        List<DailyLiquidation> expected = List.of(
                DailyLiquidation.restore(1L, Mechanic.restore(1L, "Jose", true), date, new BigDecimal("100000.00"), 2)
        );
        when(dailyLiquidationRepository.findAll(1L, date)).thenReturn(expected);

        List<DailyLiquidation> result = liquidationService.list(1L, date);

        assertEquals(expected, result);
        verify(dailyLiquidationRepository).findAll(1L, date);
    }

    @Test
    void create_singleMechanic_createsAndSavesLiquidation() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        DailyLiquidation saved = DailyLiquidation.restore(10L, mechanic, date, new BigDecimal("160000.00"), 4);

        when(mechanicService.getById(1L)).thenReturn(mechanic);
        when(dailyLiquidationRepository.existsByMechanicAndDate(1L, date)).thenReturn(false);
        when(invoiceRepository.sumActiveServiceLaborByMechanicAndDate(1L, date)).thenReturn(new BigDecimal("160000.00"));
        when(invoiceRepository.countActiveServiceInvoicesByMechanicAndDate(1L, date)).thenReturn(4);
        when(dailyLiquidationRepository.save(any(DailyLiquidation.class))).thenReturn(saved);

        List<DailyLiquidation> result = liquidationService.create(date, 1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals(new BigDecimal("112000.00"), result.get(0).mechanicShare());
        verify(mechanicService).getById(1L);
        verify(dailyLiquidationRepository).existsByMechanicAndDate(1L, date);
        verify(invoiceRepository).sumActiveServiceLaborByMechanicAndDate(1L, date);
        verify(invoiceRepository).countActiveServiceInvoicesByMechanicAndDate(1L, date);
        verify(dailyLiquidationRepository).save(any(DailyLiquidation.class));
    }

    @Test
    void create_singleMechanic_whenAlreadyExists_throwsConflict() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);

        when(mechanicService.getById(1L)).thenReturn(mechanic);
        when(dailyLiquidationRepository.existsByMechanicAndDate(1L, date)).thenReturn(true);

        assertThrows(LiquidationAlreadyExistsException.class, () -> liquidationService.create(date, 1L));
        verify(mechanicService).getById(1L);
        verify(dailyLiquidationRepository).existsByMechanicAndDate(1L, date);
    }

    @Test
    void create_singleMechanic_whenMissing_throwsMechanicNotFound() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        when(mechanicService.getById(99L)).thenThrow(new MechanicNotFoundException(99L));

        assertThrows(MechanicNotFoundException.class, () -> liquidationService.create(date, 99L));
        verify(mechanicService).getById(99L);
    }

    @Test
    void create_allMechanics_createsForEachEligibleMechanic() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        Mechanic mechanic1 = Mechanic.restore(1L, "Jose", true);
        Mechanic mechanic2 = Mechanic.restore(2L, "Andres", true);

        when(invoiceRepository.findActiveMechanicIdsWithActiveServiceInvoicesByDate(date)).thenReturn(List.of(2L, 1L));

        when(mechanicService.getById(1L)).thenReturn(mechanic1);
        when(mechanicService.getById(2L)).thenReturn(mechanic2);

        when(dailyLiquidationRepository.existsByMechanicAndDate(anyLong(), any(LocalDate.class))).thenReturn(false);

        when(invoiceRepository.sumActiveServiceLaborByMechanicAndDate(1L, date)).thenReturn(new BigDecimal("100000.00"));
        when(invoiceRepository.countActiveServiceInvoicesByMechanicAndDate(1L, date)).thenReturn(2);
        when(invoiceRepository.sumActiveServiceLaborByMechanicAndDate(2L, date)).thenReturn(new BigDecimal("50000.00"));
        when(invoiceRepository.countActiveServiceInvoicesByMechanicAndDate(2L, date)).thenReturn(1);

        when(dailyLiquidationRepository.save(any(DailyLiquidation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<DailyLiquidation> result = liquidationService.create(date, null);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.mechanic().id().equals(1L) && r.invoiceCount() == 2));
        assertTrue(result.stream().anyMatch(r -> r.mechanic().id().equals(2L) && r.invoiceCount() == 1));
        verify(invoiceRepository).findActiveMechanicIdsWithActiveServiceInvoicesByDate(date);
    }

    @Test
    void create_whenDateMissing_throwsValidationError() {
        assertThrows(IllegalArgumentException.class, () -> liquidationService.create(null, 1L));
    }
}
