package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.Invoice;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InvoiceCancelResponse(
        Long id,
        @JsonProperty("is_cancelled") boolean isCancelled
) {
    public static InvoiceCancelResponse fromDomain(Invoice invoice) {
        return new InvoiceCancelResponse(invoice.id(), invoice.cancelled());
    }
}
