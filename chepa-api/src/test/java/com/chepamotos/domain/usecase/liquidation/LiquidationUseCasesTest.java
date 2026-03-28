package com.chepamotos.domain.usecase.liquidation;

import com.chepamotos.domain.exception.LiquidationAlreadyExistsException;
import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.port.DailyLiquidationRepository;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.domain.port.MechanicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiquidationUseCasesTest {

    @Mock
    private DailyLiquidationRepository dailyLiquidationRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @Test
    void listUseCase_execute_returnsRepositoryResults() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        List<DailyLiquidation> expected = List.of(
                sampleLiquidation(1L, 1L, "Jose", date),
                sampleLiquidation(2L, 2L, "Carlos", date)
        );
        when(dailyLiquidationRepository.findAll(null, date)).thenReturn(expected);

        ListLiquidationsUseCase useCase = new ListLiquidationsUseCase(dailyLiquidationRepository);
        List<DailyLiquidation> result = useCase.execute(null, date);

        assertEquals(expected, result);
        verify(dailyLiquidationRepository).findAll(null, date);
    }

    @Test
    void listUseCase_withMechanicIdFilter_returnsMechanicLiquidations() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        List<DailyLiquidation> expected = List.of(sampleLiquidation(1L, 1L, "Jose", date));
        when(dailyLiquidationRepository.findAll(1L, date)).thenReturn(expected);

        ListLiquidationsUseCase useCase = new ListLiquidationsUseCase(dailyLiquidationRepository);
        List<DailyLiquidation> result = useCase.execute(1L, date);

        assertEquals(expected, result);
        verify(dailyLiquidationRepository).findAll(1L, date);
    }

    @Test
    void createUseCase_singleMechanic_whenMechanicExistsAndNoLiquidation_createsAndSaves() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));
        when(dailyLiquidationRepository.existsByMechanicAndDate(1L, date)).thenReturn(false);
        when(invoiceRepository.sumActiveServiceLaborByMechanicAndDate(1L, date))
                .thenReturn(new BigDecimal("100000.00"));
        when(invoiceRepository.countActiveServiceInvoicesByMechanicAndDate(1L, date))
                .thenReturn(4);
        when(dailyLiquidationRepository.save(any(DailyLiquidation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateLiquidationUseCase useCase = new CreateLiquidationUseCase(
                dailyLiquidationRepository,
                invoiceRepository,
                mechanicRepository
        );

        List<DailyLiquidation> result = useCase.execute(date, 1L);

        assertEquals(1, result.size());
        DailyLiquidation created = result.get(0);
        assertEquals(1L, created.mechanic().id());
        assertEquals(date, created.date());
        assertEquals(new BigDecimal("100000.00"), created.totalRevenue());
        assertEquals(new BigDecimal("70000.00"), created.mechanicShare());
        assertEquals(new BigDecimal("30000.00"), created.shopShare());
        assertEquals(4, created.invoiceCount());

        verify(mechanicRepository).findById(1L);
        verify(dailyLiquidationRepository).existsByMechanicAndDate(1L, date);
        verify(invoiceRepository).sumActiveServiceLaborByMechanicAndDate(1L, date);
        verify(invoiceRepository).countActiveServiceInvoicesByMechanicAndDate(1L, date);
        verify(dailyLiquidationRepository).save(any(DailyLiquidation.class));
    }

    @Test
    void createUseCase_singleMechanic_whenMechanicMissing_throwsMechanicNotFoundException() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        when(mechanicRepository.findById(99L)).thenReturn(Optional.empty());

        CreateLiquidationUseCase useCase = new CreateLiquidationUseCase(
                dailyLiquidationRepository,
                invoiceRepository,
                mechanicRepository
        );

        assertThrows(MechanicNotFoundException.class, () -> useCase.execute(date, 99L));

        verify(mechanicRepository).findById(99L);
        verify(dailyLiquidationRepository, never()).existsByMechanicAndDate(anyLong(), any(LocalDate.class));
        verify(dailyLiquidationRepository, never()).save(any(DailyLiquidation.class));
    }

    @Test
    void createUseCase_singleMechanic_whenLiquidationAlreadyExists_throwsConflict() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));
        when(dailyLiquidationRepository.existsByMechanicAndDate(1L, date)).thenReturn(true);

        CreateLiquidationUseCase useCase = new CreateLiquidationUseCase(
                dailyLiquidationRepository,
                invoiceRepository,
                mechanicRepository
        );

        assertThrows(LiquidationAlreadyExistsException.class, () -> useCase.execute(date, 1L));

        verify(mechanicRepository).findById(1L);
        verify(dailyLiquidationRepository).existsByMechanicAndDate(1L, date);
        verify(invoiceRepository, never()).sumActiveServiceLaborByMechanicAndDate(anyLong(), any(LocalDate.class));
        verify(dailyLiquidationRepository, never()).save(any(DailyLiquidation.class));
    }

    @Test
    void createUseCase_allMechanics_whenEligibleMechanicsWithoutLiquidations_createsForEach() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        Mechanic mechanic1 = Mechanic.restore(1L, "Jose", true);
        Mechanic mechanic2 = Mechanic.restore(2L, "Carlos", true);

        when(invoiceRepository.findActiveMechanicIdsWithActiveServiceInvoicesByDate(date))
                .thenReturn(List.of(1L, 2L));
        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic1));
        when(mechanicRepository.findById(2L)).thenReturn(Optional.of(mechanic2));
        when(dailyLiquidationRepository.existsByMechanicAndDate(1L, date)).thenReturn(false);
        when(dailyLiquidationRepository.existsByMechanicAndDate(2L, date)).thenReturn(false);
        when(invoiceRepository.sumActiveServiceLaborByMechanicAndDate(1L, date))
                .thenReturn(new BigDecimal("100000.00"));
        when(invoiceRepository.sumActiveServiceLaborByMechanicAndDate(2L, date))
                .thenReturn(new BigDecimal("50000.00"));
        when(invoiceRepository.countActiveServiceInvoicesByMechanicAndDate(1L, date)).thenReturn(4);
        when(invoiceRepository.countActiveServiceInvoicesByMechanicAndDate(2L, date)).thenReturn(2);
        when(dailyLiquidationRepository.save(any(DailyLiquidation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateLiquidationUseCase useCase = new CreateLiquidationUseCase(
                dailyLiquidationRepository,
                invoiceRepository,
                mechanicRepository
        );

        List<DailyLiquidation> result = useCase.execute(date, null);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).mechanic().id());
        assertEquals(2L, result.get(1).mechanic().id());
        assertEquals(new BigDecimal("100000.00"), result.get(0).totalRevenue());
        assertEquals(new BigDecimal("50000.00"), result.get(1).totalRevenue());

        verify(invoiceRepository).findActiveMechanicIdsWithActiveServiceInvoicesByDate(date);
        verify(mechanicRepository).findById(1L);
        verify(mechanicRepository).findById(2L);
        verify(dailyLiquidationRepository, times(2)).save(any(DailyLiquidation.class));
        verify(invoiceRepository).sumActiveServiceLaborByMechanicAndDate(1L, date);
        verify(invoiceRepository).sumActiveServiceLaborByMechanicAndDate(2L, date);
        verify(invoiceRepository).countActiveServiceInvoicesByMechanicAndDate(1L, date);
        verify(invoiceRepository).countActiveServiceInvoicesByMechanicAndDate(2L, date);
    }

    @Test
    void createUseCase_allMechanics_whenNoEligibleMechanics_returnsEmptyList() {
        LocalDate date = LocalDate.of(2026, 1, 28);

        when(invoiceRepository.findActiveMechanicIdsWithActiveServiceInvoicesByDate(date))
                .thenReturn(List.of());

        CreateLiquidationUseCase useCase = new CreateLiquidationUseCase(
                dailyLiquidationRepository,
                invoiceRepository,
                mechanicRepository
        );

        List<DailyLiquidation> result = useCase.execute(date, null);

        assertEquals(0, result.size());
        verify(invoiceRepository).findActiveMechanicIdsWithActiveServiceInvoicesByDate(date);
        verify(mechanicRepository, never()).findById(anyLong());
        verify(dailyLiquidationRepository, never()).save(any(DailyLiquidation.class));
    }

    @Test
    void createUseCase_allMechanics_whenOneMechanicHasLiquidation_throwsConflictForFirst() {
        LocalDate date = LocalDate.of(2026, 1, 28);
        Mechanic mechanic1 = Mechanic.restore(1L, "Jose", true);

        when(invoiceRepository.findActiveMechanicIdsWithActiveServiceInvoicesByDate(date))
                .thenReturn(List.of(1L, 2L));
        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic1));
        when(dailyLiquidationRepository.existsByMechanicAndDate(1L, date)).thenReturn(true);

        CreateLiquidationUseCase useCase = new CreateLiquidationUseCase(
                dailyLiquidationRepository,
                invoiceRepository,
                mechanicRepository
        );

        assertThrows(LiquidationAlreadyExistsException.class, () -> useCase.execute(date, null));

        verify(mechanicRepository).findById(1L);
        verify(dailyLiquidationRepository).existsByMechanicAndDate(1L, date);
        verify(dailyLiquidationRepository, never()).save(any(DailyLiquidation.class));
    }

    @Test
    void createUseCase_whenDateIsNull_throwsIllegalArgument() {
        CreateLiquidationUseCase useCase = new CreateLiquidationUseCase(
                dailyLiquidationRepository,
                invoiceRepository,
                mechanicRepository
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, 1L));

        verify(mechanicRepository, never()).findById(anyLong());
        verify(dailyLiquidationRepository, never()).save(any(DailyLiquidation.class));
    }

    private static DailyLiquidation sampleLiquidation(Long id, Long mechanicId, String mechanicName, LocalDate date) {
        return DailyLiquidation.restore(
                id,
                Mechanic.restore(mechanicId, mechanicName, true),
                date,
                new BigDecimal("100000.00"),
                4
        );
    }
}
