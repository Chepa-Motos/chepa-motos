package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.port.InvoiceRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class ListInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    public ListInvoicesUseCase(InvoiceRepository invoiceRepository, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.clock = clock;
    }

    public List<Invoice> execute(LocalDate date, InvoiceType type, Long mechanicId, boolean cancelled) {
        LocalDate effectiveDate = date == null ? LocalDate.now(clock) : date;
        return invoiceRepository.findAllByFilters(effectiveDate, type, mechanicId, cancelled);
    }
}