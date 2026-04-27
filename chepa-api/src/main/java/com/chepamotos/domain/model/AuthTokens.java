package com.chepamotos.domain.model;

import java.util.Objects;

public final class AuthTokens {

    private final String tokenType;
    private final String accessToken;
    private final long accessTokenExpiresInSeconds;
    private final String refreshToken;

    private AuthTokens(String tokenType, String accessToken, long accessTokenExpiresInSeconds, String refreshToken) {
        this.tokenType = requireNonBlank(tokenType, "Token type is required");
        this.accessToken = requireNonBlank(accessToken, "Access token is required");
        if (accessTokenExpiresInSeconds <= 0) {
            throw new IllegalArgumentException("Access token expiration must be greater than zero");
        }
        this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
        this.refreshToken = requireNonBlank(refreshToken, "Refresh token is required");
    }

    public static AuthTokens of(AccessToken accessToken, String refreshToken) {
        Objects.requireNonNull(accessToken, "Access token payload is required");
        return new AuthTokens("Bearer", accessToken.token(), accessToken.expiresInSeconds(), refreshToken);
    }

    public String tokenType() {
        return tokenType;
    }

    public String accessToken() {
        return accessToken;
    }

    public long accessTokenExpiresInSeconds() {
        return accessTokenExpiresInSeconds;
    }

    public String refreshToken() {
        return refreshToken;
    }

    private static String requireNonBlank(String raw, String message) {
        String value = Objects.requireNonNull(raw, message).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
