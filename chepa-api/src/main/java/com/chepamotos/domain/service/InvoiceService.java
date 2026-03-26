package com.chepamotos.domain.service;

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
    public Invoice create(InvoiceType type, Long mechanicId, String vehiclePlate, String vehicleModel, String buyerName, BigDecimal laborAmount, List<InvoiceItemData> itemData) {
        List<InvoiceItem> items = itemData.stream()
                .map(i -> InvoiceItem.createNew(i.description(), i.quantity(), i.unitPrice()))
                .toList();

        Invoice invoice;
        if (type == InvoiceType.SERVICE) {
            var mechanic = mechanicService.getById(mechanicId);
            var vehicle = vehicleService.resolveForServiceInvoice(vehiclePlate, vehicleModel);
            invoice = Invoice.createService(mechanic, vehicle, laborAmount, items);
        } else {
            invoice = Invoice.createDelivery(buyerName, items);
        }

        return invoiceRepository.save(invoice);
    }
}
