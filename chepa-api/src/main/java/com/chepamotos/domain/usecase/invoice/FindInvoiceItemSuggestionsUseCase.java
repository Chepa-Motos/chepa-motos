package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.port.InvoiceRepository;

import java.util.List;

public class FindInvoiceItemSuggestionsUseCase {

    private final InvoiceRepository invoiceRepository;

    public FindInvoiceItemSuggestionsUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<InvoiceItem> execute(String model, String descriptionPrefix) {
        return invoiceRepository.findSuggestionsByModelAndDescription(model, descriptionPrefix);
    }
}
