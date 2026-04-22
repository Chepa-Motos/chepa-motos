package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.port.in.InvoiceApplicationUseCase;
import com.chepamotos.domain.usecase.invoice.CancelInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateDeliveryInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateServiceInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.FindInvoiceItemSuggestionsUseCase;
import com.chepamotos.domain.usecase.invoice.GetInvoiceByIdUseCase;
import com.chepamotos.domain.usecase.invoice.ListInvoicesUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceApplicationService implements InvoiceApplicationUseCase {

    private final ListInvoicesUseCase listInvoicesUseCase;
    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final CancelInvoiceUseCase cancelInvoiceUseCase;
    private final CreateServiceInvoiceUseCase createServiceInvoiceUseCase;
    private final CreateDeliveryInvoiceUseCase createDeliveryInvoiceUseCase;
    private final FindInvoiceItemSuggestionsUseCase findInvoiceItemSuggestionsUseCase;

    public InvoiceApplicationService(
            ListInvoicesUseCase listInvoicesUseCase,
            GetInvoiceByIdUseCase getInvoiceByIdUseCase,
            CancelInvoiceUseCase cancelInvoiceUseCase,
            CreateServiceInvoiceUseCase createServiceInvoiceUseCase,
            CreateDeliveryInvoiceUseCase createDeliveryInvoiceUseCase,
            FindInvoiceItemSuggestionsUseCase findInvoiceItemSuggestionsUseCase) {
        this.listInvoicesUseCase = listInvoicesUseCase;
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.cancelInvoiceUseCase = cancelInvoiceUseCase;
        this.createServiceInvoiceUseCase = createServiceInvoiceUseCase;
        this.createDeliveryInvoiceUseCase = createDeliveryInvoiceUseCase;
        this.findInvoiceItemSuggestionsUseCase = findInvoiceItemSuggestionsUseCase;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Invoice> listAll() {
        return listInvoicesUseCase.execute();
    }

    @Transactional(readOnly = true)
    @Override
    public Invoice getById(Long invoiceId) {
        return getInvoiceByIdUseCase.execute(invoiceId);
    }

    @Transactional
    @Override
    public Invoice cancel(Long invoiceId) {
        return cancelInvoiceUseCase.execute(invoiceId);
    }

    @Transactional
    @Override
    public Invoice createService(
            Long mechanicId,
            String vehiclePlate,
            String vehicleModel,
            BigDecimal laborAmount,
            List<InvoiceItemInput> itemInputs) {
        return createServiceInvoiceUseCase.execute(mechanicId, vehiclePlate, vehicleModel, laborAmount, itemInputs);
    }

    @Transactional
    @Override
    public Invoice createDelivery(String buyerName, List<InvoiceItemInput> itemInputs) {
        return createDeliveryInvoiceUseCase.execute(buyerName, itemInputs);
    }

    @Transactional(readOnly = true)
    @Override
    public List<InvoiceItem> findSuggestions(String model, String q) {
        return findInvoiceItemSuggestionsUseCase.execute(model, q);
    }
}