package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.usecase.invoice.CancelInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateDeliveryInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateServiceInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.GetInvoiceByIdUseCase;
import com.chepamotos.domain.usecase.invoice.ListInvoicesUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceApplicationService {

    private final ListInvoicesUseCase listInvoicesUseCase;
    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final CancelInvoiceUseCase cancelInvoiceUseCase;
    private final CreateServiceInvoiceUseCase createServiceInvoiceUseCase;
    private final CreateDeliveryInvoiceUseCase createDeliveryInvoiceUseCase;

    public InvoiceApplicationService(
            ListInvoicesUseCase listInvoicesUseCase,
            GetInvoiceByIdUseCase getInvoiceByIdUseCase,
            CancelInvoiceUseCase cancelInvoiceUseCase,
            CreateServiceInvoiceUseCase createServiceInvoiceUseCase,
            CreateDeliveryInvoiceUseCase createDeliveryInvoiceUseCase) {
        this.listInvoicesUseCase = listInvoicesUseCase;
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.cancelInvoiceUseCase = cancelInvoiceUseCase;
        this.createServiceInvoiceUseCase = createServiceInvoiceUseCase;
        this.createDeliveryInvoiceUseCase = createDeliveryInvoiceUseCase;
    }

    @Transactional(readOnly = true)
    public List<Invoice> listAll() {
        return listInvoicesUseCase.execute();
    }

    @Transactional(readOnly = true)
    public Invoice getById(Long invoiceId) {
        return getInvoiceByIdUseCase.execute(invoiceId);
    }

    @Transactional
    public Invoice cancel(Long invoiceId) {
        return cancelInvoiceUseCase.execute(invoiceId);
    }

    @Transactional
    public Invoice createService(
            Long mechanicId,
            String vehiclePlate,
            String vehicleModel,
            BigDecimal laborAmount,
            List<InvoiceItemInput> itemInputs) {
        return createServiceInvoiceUseCase.execute(mechanicId, vehiclePlate, vehicleModel, laborAmount, itemInputs);
    }

    @Transactional
    public Invoice createDelivery(String buyerName, List<InvoiceItemInput> itemInputs) {
        return createDeliveryInvoiceUseCase.execute(buyerName, itemInputs);
    }
}