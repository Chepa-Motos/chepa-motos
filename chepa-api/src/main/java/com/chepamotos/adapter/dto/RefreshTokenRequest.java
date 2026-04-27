package com.chepamotos.adapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "refresh_token is required")
        @Size(max = 500, message = "refresh_token cannot exceed 500 characters")
        String refreshToken
) {
}
