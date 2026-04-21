package com.chepamotos.domain.service;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceItemInput;

import java.math.BigDecimal;
import java.util.List;

public interface InvoiceApplicationUseCase {

    List<Invoice> listAll();

    Invoice getById(Long invoiceId);

    Invoice cancel(Long invoiceId);

    Invoice createService(Long mechanicId, String vehiclePlate, String vehicleModel, BigDecimal laborAmount, List<InvoiceItemInput> itemInputs);

    Invoice createDelivery(String buyerName, List<InvoiceItemInput> itemInputs);

    List<InvoiceItem> findSuggestions(String model, String q);
}
