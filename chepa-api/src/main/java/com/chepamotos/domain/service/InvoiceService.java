package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.InvoiceAlreadyCancelledException;
import com.chepamotos.domain.exception.InvoiceNotFoundException;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.port.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceService {

    public record InvoiceItemData(String description, BigDecimal quantity, BigDecimal unitPrice) {}

    private final InvoiceRepository invoiceRepository;
    private final MechanicService mechanicService;
    private final VehicleService vehicleService;

    public InvoiceService(InvoiceRepository invoiceRepository, MechanicService mechanicService, VehicleService vehicleService) {
        this.invoiceRepository = invoiceRepository;
        this.mechanicService = mechanicService;
        this.vehicleService = vehicleService;
    }

    @Transactional(readOnly = true)
    public List<Invoice> listAll() {
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Invoice getById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    @Transactional
    public Invoice cancel(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (invoice.cancelled()) {
            throw new InvoiceAlreadyCancelledException(invoiceId);
        }

        return invoiceRepository.save(invoice.cancel());
    }

    @Transactional
    public Invoice create(InvoiceType type, Long mechanicId, String vehiclePlate, String vehicleModel, String buyerName, BigDecimal laborAmount, List<InvoiceItemData> itemData) {
        if (type == null) {
            throw new IllegalArgumentException("invoice_type is required");
        }

        if (itemData == null) {
            throw new IllegalArgumentException("Invoice items are required");
        }

        List<InvoiceItem> items = itemData.stream()
                .map(i -> InvoiceItem.createNew(i.description(), i.quantity(), i.unitPrice()))
                .toList();

        Invoice invoice = switch (type) {
            case SERVICE -> {
                var mechanic = mechanicService.getById(mechanicId);
                var vehicle = vehicleService.resolveForServiceInvoice(vehiclePlate, vehicleModel);
                yield Invoice.createService(mechanic, vehicle, laborAmount, items);
            }
            case DELIVERY -> Invoice.createDelivery(buyerName, items);
        };

        return invoiceRepository.save(invoice);
    }
}
