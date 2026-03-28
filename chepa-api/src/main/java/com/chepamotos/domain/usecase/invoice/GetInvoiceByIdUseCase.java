package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.exception.InvoiceNotFoundException;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.port.InvoiceRepository;

public class GetInvoiceByIdUseCase {

    private final InvoiceRepository invoiceRepository;

    public GetInvoiceByIdUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice execute(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }
}