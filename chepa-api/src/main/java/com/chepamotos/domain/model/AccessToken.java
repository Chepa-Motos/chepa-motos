package com.chepamotos.domain.model;

import java.util.Objects;

public final class AccessToken {

    private final String token;
    private final long expiresInSeconds;

    public AccessToken(String token, long expiresInSeconds) {
        this.token = Objects.requireNonNull(token, "Access token is required").trim();
        if (this.token.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be blank");
        }
        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException("Access token expiration must be greater than zero");
        }
        this.expiresInSeconds = expiresInSeconds;
    }

    public String token() {
        return token;
    }

    public long expiresInSeconds() {
        return expiresInSeconds;
    }
}
