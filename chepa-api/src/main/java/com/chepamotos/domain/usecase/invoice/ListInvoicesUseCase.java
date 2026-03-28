package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.port.InvoiceRepository;

import java.util.List;

public class ListInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;

    public ListInvoicesUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Invoice> execute() {
        return invoiceRepository.findAll();
    }
}