package com.chepamotos.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record UpdateMechanicStatusRequest(
        @JsonProperty("is_active")
        @NotNull(message = "is_active is required")
        Boolean isActive
) {
}
