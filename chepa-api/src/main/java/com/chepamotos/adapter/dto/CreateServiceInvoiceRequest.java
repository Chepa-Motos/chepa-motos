package com.chepamotos.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record CreateServiceInvoiceRequest(
        @NotNull(message = "mechanic_id is required")
        @JsonProperty("mechanic_id")
        Long mechanicId,

        @NotNull(message = "vehicle_plate is required")
        @NotBlank(message = "vehicle_plate cannot be blank")
        @JsonProperty("vehicle_plate")
        String vehiclePlate,

        @NotNull(message = "model is required")
        @NotBlank(message = "model cannot be blank")
        String model,

        @NotNull(message = "labor_amount is required")
        @PositiveOrZero(message = "labor_amount cannot be negative")
        @JsonProperty("labor_amount")
        BigDecimal laborAmount,

        @NotNull(message = "items are required")
        @NotEmpty(message = "invoice requires at least one item")
        @Valid
        List<CreateInvoiceItemRequest> items
) {
}
