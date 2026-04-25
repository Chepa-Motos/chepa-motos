package com.chepamotos.domain.port.in;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.model.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceApplicationUseCase {

    List<Invoice> list(LocalDate date, InvoiceType type, Long mechanicId, boolean cancelled);

    Invoice getById(Long invoiceId);

    Invoice cancel(Long invoiceId);

    Invoice createService(Long mechanicId, String vehiclePlate, String vehicleModel, BigDecimal laborAmount, List<InvoiceItemInput> itemInputs);

    Invoice createDelivery(String buyerName, List<InvoiceItemInput> itemInputs);

    List<InvoiceItem> findSuggestions(String model, String q);
}