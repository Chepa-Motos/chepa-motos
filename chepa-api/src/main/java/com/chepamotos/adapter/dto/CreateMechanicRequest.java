package com.chepamotos.adapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMechanicRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name cannot exceed 100 characters")
        String name
) {
}
