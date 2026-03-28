package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.exception.InvoiceAlreadyCancelledException;
import com.chepamotos.domain.exception.InvoiceNotFoundException;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.port.InvoiceRepository;

public class CancelInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;

    public CancelInvoiceUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice execute(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (invoice.cancelled()) {
            throw new InvoiceAlreadyCancelledException(invoiceId);
        }

        return invoiceRepository.save(invoice.cancel());
    }
}