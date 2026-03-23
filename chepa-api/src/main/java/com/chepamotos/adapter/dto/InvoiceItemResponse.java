package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.InvoiceItem;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        String description,
        BigDecimal quantity,
        @JsonProperty("unit_price") BigDecimal unitPrice,
        BigDecimal subtotal
) {

    public static InvoiceItemResponse fromDomain(InvoiceItem item) {
        return new InvoiceItemResponse(item.id(), item.description(), item.quantity(), item.unitPrice(), item.subtotal());
    }
}
