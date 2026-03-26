package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.Invoice;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceResponse(
        Long id,
        @JsonProperty("invoice_type") String invoiceType,
        MechanicResponse mechanic,
        VehicleResponse vehicle,
        @JsonProperty("buyer_name") String buyerName,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("labor_amount") BigDecimal laborAmount,
        @JsonProperty("total_amount") BigDecimal totalAmount,
        @JsonProperty("is_cancelled") boolean isCancelled,
        List<InvoiceItemResponse> items
) {

    public static InvoiceResponse fromDomain(Invoice invoice) {
        return new InvoiceResponse(
                invoice.id(),
                invoice.type().name(),
                invoice.mechanic() != null ? MechanicResponse.fromDomain(invoice.mechanic()) : null,
                invoice.vehicle() != null ? VehicleResponse.fromDomain(invoice.vehicle()) : null,
                invoice.buyerName(),
                invoice.createdAt().toString(),
                invoice.laborAmount(),
                invoice.totalAmount(),
                invoice.cancelled(),
                invoice.items().stream().map(InvoiceItemResponse::fromDomain).toList()
        );
    }
}
