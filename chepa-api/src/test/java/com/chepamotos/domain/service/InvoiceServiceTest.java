package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.InvoiceNotFoundException;
import com.chepamotos.domain.exception.InvoiceAlreadyCancelledException;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MechanicService mechanicService;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void listAll_returnsRepositoryResults() {
        List<Invoice> expected = List.of(sampleServiceInvoice(1L));
        when(invoiceRepository.findAll()).thenReturn(expected);

        List<Invoice> result = invoiceService.listAll();

        assertEquals(expected, result);
        verify(invoiceRepository).findAll();
    }

    @Test
    void getById_whenExists_returnsInvoice() {
        Invoice expected = sampleServiceInvoice(1L);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(expected));

        Invoice result = invoiceService.getById(1L);

        assertEquals(expected, result);
        verify(invoiceRepository).findById(1L);
    }

    @Test
    void getById_whenMissing_throwsInvoiceNotFoundException() {
        when(invoiceRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(InvoiceNotFoundException.class, () -> invoiceService.getById(99L));
        verify(invoiceRepository).findById(99L);
    }

    @Test
    void cancel_whenActiveInvoice_marksCancelledAndSaves() {
        Invoice active = sampleServiceInvoice(10L);
        Invoice cancelled = Invoice.restore(
                10L,
                InvoiceType.SERVICE,
                active.mechanic(),
                active.vehicle(),
                active.buyerName(),
                active.createdAt(),
                active.laborAmount(),
                true,
                active.items()
        );

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(active));
        when(invoiceRepository.save(any())).thenReturn(cancelled);

        Invoice result = invoiceService.cancel(10L);
        ArgumentCaptor<Invoice> savedInvoiceCaptor = ArgumentCaptor.forClass(Invoice.class);

        assertTrue(result.cancelled());
        verify(invoiceRepository).findById(10L);
        verify(invoiceRepository).save(savedInvoiceCaptor.capture());

        Invoice savedArgument = savedInvoiceCaptor.getValue();
        assertEquals(10L, savedArgument.id());
        assertTrue(savedArgument.cancelled());
    }

    @Test
    void cancel_whenNotFound_throwsInvoiceNotFoundException() {
        when(invoiceRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(InvoiceNotFoundException.class, () -> invoiceService.cancel(999L));
        verify(invoiceRepository).findById(999L);
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsInvoiceAlreadyCancelledException() {
        Invoice cancelled = Invoice.restore(
                20L,
                InvoiceType.DELIVERY,
                null,
                null,
                "Buyer",
                LocalDateTime.now(),
                BigDecimal.ZERO,
                true,
                List.of(InvoiceItem.restore(1L, "Item", BigDecimal.ONE, new BigDecimal("1000.00")))
        );

        when(invoiceRepository.findById(20L)).thenReturn(Optional.of(cancelled));

        assertThrows(InvoiceAlreadyCancelledException.class, () -> invoiceService.cancel(20L));
        verify(invoiceRepository).findById(20L);
    }

    @Test
    void create_serviceInvoice_resolvesAndSaves() {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        Vehicle vehicle = Vehicle.restore(1L, "BXR42H", "Boxer 150 2021");
        Invoice saved = sampleServiceInvoice(5L);

        when(mechanicService.getById(1L)).thenReturn(mechanic);
        when(vehicleService.resolveForServiceInvoice("BXR42H", "Boxer 150 2021")).thenReturn(vehicle);
        when(invoiceRepository.save(any())).thenReturn(saved);

        List<InvoiceService.InvoiceItemData> itemData = List.of(
                new InvoiceService.InvoiceItemData("Freno delantero", BigDecimal.ONE, new BigDecimal("36900.00"))
        );

        Invoice result = invoiceService.create(InvoiceType.SERVICE, 1L, "BXR42H", "Boxer 150 2021", null, new BigDecimal("45000.00"), itemData);

        assertEquals(saved, result);
        verify(mechanicService).getById(1L);
        verify(vehicleService).resolveForServiceInvoice("BXR42H", "Boxer 150 2021");
        verify(invoiceRepository).save(any());
    }

    @Test
    void create_deliveryInvoice_savesWithBuyerName() {
        Invoice saved = sampleDeliveryInvoice(6L);
        when(invoiceRepository.save(any())).thenReturn(saved);

        List<InvoiceService.InvoiceItemData> itemData = List.of(
                new InvoiceService.InvoiceItemData("Boxer 150 Palanca de freno", new BigDecimal("2"), new BigDecimal("18500.00"))
        );

        Invoice result = invoiceService.create(InvoiceType.DELIVERY, null, null, null, "Talleres La 80", BigDecimal.ZERO, itemData);


        assertEquals(saved, result);
        verify(invoiceRepository).save(any());
    }

    @Test
    void create_whenEmptyItems_throwsDomainValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            invoiceService.create(InvoiceType.DELIVERY, null, null, null, "Buyer", BigDecimal.ZERO, List.of())
        );

    }

    @Test
    void create_whenBlankBuyerName_throwsDomainValidation() {
        List<InvoiceService.InvoiceItemData> itemData = List.of(
                new InvoiceService.InvoiceItemData("Item", BigDecimal.ONE, new BigDecimal("1000.00"))
        );

        assertThrows(IllegalArgumentException.class, () ->
            invoiceService.create(InvoiceType.DELIVERY, null, null, null, "   ", BigDecimal.ZERO, itemData));

    }

    @Test
    void create_whenItemDataIsNull_throwsValidationError() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                invoiceService.create(InvoiceType.DELIVERY, null, null, null, "Buyer", BigDecimal.ZERO, null));

        assertTrue(ex.getMessage().contains("Invoice items are required"));
    }

    @Test
    void create_whenTypeIsNull_throwsValidationError() {
        List<InvoiceService.InvoiceItemData> itemData = List.of(
                new InvoiceService.InvoiceItemData("Item", BigDecimal.ONE, new BigDecimal("1000.00"))
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                invoiceService.create(null, null, null, null, "Buyer", BigDecimal.ZERO, itemData));

        assertTrue(ex.getMessage().contains("invoice_type is required"));
    }

    private Invoice sampleServiceInvoice(Long id) {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        Vehicle vehicle = Vehicle.restore(1L, "BXR42H", "Boxer 150 2021");
        List<InvoiceItem> items = List.of(InvoiceItem.restore(1L, "Freno delantero", BigDecimal.ONE, new BigDecimal("36900.00")));
        return Invoice.restore(id, InvoiceType.SERVICE, mechanic, vehicle, null, LocalDateTime.now(), new BigDecimal("45000.00"), false, items);
    }

    private Invoice sampleDeliveryInvoice(Long id) {
        List<InvoiceItem> items = List.of(InvoiceItem.restore(1L, "Palanca de freno", new BigDecimal("2"), new BigDecimal("18500.00")));
        return Invoice.restore(id, InvoiceType.DELIVERY, null, null, "Talleres La 80", LocalDateTime.now(), BigDecimal.ZERO, false, items);
    }
}
