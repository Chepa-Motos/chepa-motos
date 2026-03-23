package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.InvoiceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateInvoiceRequest(
        @NotNull(message = "invoice_type is required")
        @JsonProperty("invoice_type") InvoiceType invoiceType,

        @JsonProperty("mechanic_id") Long mechanicId,

        @JsonProperty("vehicle_plate") String vehiclePlate,

        @JsonProperty("buyer_name") String buyerName,

        @JsonProperty("labor_amount") BigDecimal laborAmount,

        @NotNull(message = "items are required")
        @NotEmpty(message = "invoice requires at least one item")
        @Valid List<CreateInvoiceItemRequest> items
) {
}
