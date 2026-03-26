package com.chepamotos.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateLiquidationRequest(
        @NotNull(message = "date is required")
        LocalDate date,

        @JsonProperty("mechanic_id")
        Long mechanicId
) {
}
