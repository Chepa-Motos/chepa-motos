package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.exception.InvoiceAlreadyCancelledException;
import com.chepamotos.domain.exception.InvoiceNotFoundException;
import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.domain.port.MechanicRepository;
import com.chepamotos.domain.usecase.vehicle.ResolveVehicleForServiceInvoiceUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceUseCasesTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @Mock
    private ResolveVehicleForServiceInvoiceUseCase resolveVehicleUseCase;

    @Test
    void listUseCase_execute_returnsRepositoryResults() {
        List<Invoice> expected = List.of(deliveryInvoice(1L, false), serviceInvoice(2L, false));
        when(invoiceRepository.findAll()).thenReturn(expected);

        ListInvoicesUseCase useCase = new ListInvoicesUseCase(invoiceRepository);
        List<Invoice> result = useCase.execute();

        assertEquals(expected, result);
        verify(invoiceRepository).findAll();
    }

    @Test
    void getByIdUseCase_whenExists_returnsInvoice() {
        Invoice expected = serviceInvoice(10L, false);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(expected));

        GetInvoiceByIdUseCase useCase = new GetInvoiceByIdUseCase(invoiceRepository);
        Invoice result = useCase.execute(10L);

        assertEquals(expected, result);
        verify(invoiceRepository).findById(10L);
    }

    @Test
    void getByIdUseCase_whenMissing_throwsInvoiceNotFoundException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        GetInvoiceByIdUseCase useCase = new GetInvoiceByIdUseCase(invoiceRepository);
        assertThrows(InvoiceNotFoundException.class, () -> useCase.execute(99L));

        verify(invoiceRepository).findById(99L);
    }

    @Test
    void cancelUseCase_whenInvoiceExistsAndActive_cancelsAndSaves() {
        Invoice existing = serviceInvoice(7L, false);
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelInvoiceUseCase useCase = new CancelInvoiceUseCase(invoiceRepository);
        Invoice result = useCase.execute(7L);

        assertTrue(result.cancelled());
        verify(invoiceRepository).findById(7L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void cancelUseCase_whenInvoiceAlreadyCancelled_throwsConflictAndDoesNotSave() {
        when(invoiceRepository.findById(8L)).thenReturn(Optional.of(serviceInvoice(8L, true)));

        CancelInvoiceUseCase useCase = new CancelInvoiceUseCase(invoiceRepository);
        assertThrows(InvoiceAlreadyCancelledException.class, () -> useCase.execute(8L));

        verify(invoiceRepository).findById(8L);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void createDeliveryUseCase_mapsInputs_buildsDomainInvoice_andSaves() {
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InvoiceItemInput> itemInputs = List.of(
                new InvoiceItemInput("  Freno delantero  ", new BigDecimal("2"), new BigDecimal("45000")),
                new InvoiceItemInput("Tornillo", new BigDecimal("1"), new BigDecimal("3900"))
        );

        CreateDeliveryInvoiceUseCase useCase = new CreateDeliveryInvoiceUseCase(invoiceRepository);
        Invoice result = useCase.execute("  Talleres La 80  ", itemInputs);

        assertEquals(InvoiceType.DELIVERY, result.type());
        assertEquals("Talleres La 80", result.buyerName());
        assertEquals(new BigDecimal("0.00"), result.laborAmount());
        assertEquals(new BigDecimal("93900.00"), result.totalAmount());
        assertEquals(2, result.items().size());
        assertEquals("Freno delantero", result.items().get(0).description());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createServiceUseCase_whenMechanicExists_resolvesVehicle_buildsInvoice_andSaves() {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        Vehicle vehicle = Vehicle.restore(3L, "BXR74F", "Boxer 150 2021");

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));
        when(resolveVehicleUseCase.execute("  bxr74f ", " Boxer 150 2021 ")).thenReturn(vehicle);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InvoiceItemInput> itemInputs = List.of(
                new InvoiceItemInput(" Tornillo Leva ", new BigDecimal("1"), new BigDecimal("3900")),
                new InvoiceItemInput("Aceite", new BigDecimal("1"), new BigDecimal("25000"))
        );

        CreateServiceInvoiceUseCase useCase = new CreateServiceInvoiceUseCase(
                invoiceRepository,
                mechanicRepository,
                resolveVehicleUseCase
        );

        Invoice result = useCase.execute(
                1L,
                "  bxr74f ",
                " Boxer 150 2021 ",
                new BigDecimal("65000"),
                itemInputs
        );

        assertEquals(InvoiceType.SERVICE, result.type());
        assertEquals(1L, result.mechanic().id());
        assertEquals(3L, result.vehicle().id());
        assertEquals(new BigDecimal("65000.00"), result.laborAmount());
        assertEquals(new BigDecimal("93900.00"), result.totalAmount());
        assertEquals(2, result.items().size());
        assertEquals(new BigDecimal("3900.00"), result.items().get(0).subtotal());
        verify(mechanicRepository).findById(1L);
        verify(resolveVehicleUseCase).execute("  bxr74f ", " Boxer 150 2021 ");
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createServiceUseCase_whenMechanicMissing_throwsMechanicNotFoundException() {
        when(mechanicRepository.findById(404L)).thenReturn(Optional.empty());

        CreateServiceInvoiceUseCase useCase = new CreateServiceInvoiceUseCase(
                invoiceRepository,
                mechanicRepository,
                resolveVehicleUseCase
        );

        List<InvoiceItemInput> itemInputs = List.of(
                new InvoiceItemInput("Tornillo Leva", new BigDecimal("1"), new BigDecimal("3900"))
        );

        assertThrows(
                MechanicNotFoundException.class,
                () -> useCase.execute(404L, "ABC123", "Boxer", new BigDecimal("10000"), itemInputs)
        );

        verify(mechanicRepository).findById(404L);
        verify(resolveVehicleUseCase, never()).execute(any(String.class), any(String.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    private static Invoice serviceInvoice(Long id, boolean cancelled) {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        Vehicle vehicle = Vehicle.restore(3L, "BXR74F", "Boxer 150 2021");
        List<InvoiceItem> items = List.of(
                InvoiceItem.restore(51L, "Tornillo Leva", new BigDecimal("1"), new BigDecimal("3900"))
        );
        return Invoice.restore(
                id,
                InvoiceType.SERVICE,
                mechanic,
                vehicle,
                null,
                LocalDateTime.of(2026, 1, 28, 9, 45),
                new BigDecimal("65000"),
                cancelled,
                items
        );
    }

    private static Invoice deliveryInvoice(Long id, boolean cancelled) {
        List<InvoiceItem> items = List.of(
                InvoiceItem.restore(77L, "Suzuki 150 Palanca de Freno", new BigDecimal("2"), new BigDecimal("18500"))
        );
        return Invoice.restore(
                id,
                InvoiceType.DELIVERY,
                null,
                null,
                "Talleres La 80",
                LocalDateTime.of(2026, 1, 28, 10, 30),
                BigDecimal.ZERO,
                cancelled,
                items
        );
    }
}
