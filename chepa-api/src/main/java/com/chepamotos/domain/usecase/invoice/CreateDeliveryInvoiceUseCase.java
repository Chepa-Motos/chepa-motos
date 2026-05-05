package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.port.InvoiceRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class CreateDeliveryInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    public CreateDeliveryInvoiceUseCase(InvoiceRepository invoiceRepository, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.clock = clock;
    }

    public Invoice execute(String buyerName, List<InvoiceItemInput> itemInputs) {
        List<InvoiceItem> items = itemInputs.stream()
                .map(i -> InvoiceItem.createNew(i.description(), i.quantity(), i.unitPrice()))
                .toList();

        Invoice invoice = Invoice.createDelivery(buyerName, items, LocalDateTime.now(clock));
        return invoiceRepository.save(invoice);
    }
}