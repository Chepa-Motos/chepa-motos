package com.chepamotos.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateInvoiceItemRequest(
        @NotBlank(message = "item description is required")
        String description,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "unit_price is required")
        @PositiveOrZero(message = "unit_price cannot be negative")
        @JsonProperty("unit_price") BigDecimal unitPrice
) {
}
