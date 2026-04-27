package com.chepamotos.adapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username is required")
        @Size(max = 100, message = "username cannot exceed 100 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(max = 200, message = "password cannot exceed 200 characters")
        String password
) {
}
