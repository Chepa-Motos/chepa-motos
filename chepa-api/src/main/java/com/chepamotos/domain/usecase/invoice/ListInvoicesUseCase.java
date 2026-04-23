package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.port.InvoiceRepository;

import java.time.LocalDate;
import java.util.List;

public class ListInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;

    public ListInvoicesUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Invoice> execute(LocalDate date, InvoiceType type, Long mechanicId, boolean cancelled) {
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        return invoiceRepository.findAllByFilters(effectiveDate, type, mechanicId, cancelled);
    }
}