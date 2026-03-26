package com.chepamotos.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateDeliveryInvoiceRequest(
        @NotNull(message = "buyer_name is required")
        @NotBlank(message = "buyer_name cannot be blank")
        @JsonProperty("buyer_name")
        String buyerName,

        @NotNull(message = "items are required")
        @NotEmpty(message = "invoice requires at least one item")
        @Valid
        List<CreateInvoiceItemRequest> items
) {
}
