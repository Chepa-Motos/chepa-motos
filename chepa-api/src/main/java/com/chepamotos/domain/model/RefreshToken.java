package com.chepamotos.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class RefreshToken {

    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime revokedAt;
    private final Long replacedByTokenId;

    private RefreshToken(Long id,
                         Long userId,
                         String tokenHash,
                         LocalDateTime issuedAt,
                         LocalDateTime expiresAt,
                         LocalDateTime revokedAt,
                         Long replacedByTokenId) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "Refresh token user id is required");
        this.tokenHash = requireTokenHash(tokenHash);
        this.issuedAt = Objects.requireNonNull(issuedAt, "Refresh token issue date is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Refresh token expiration date is required");
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;

        if (!this.expiresAt.isAfter(this.issuedAt)) {
            throw new IllegalArgumentException("Refresh token expiration must be after issue date");
        }
    }

    public static RefreshToken createNew(Long userId,
                                         String tokenHash,
                                         LocalDateTime issuedAt,
                                         LocalDateTime expiresAt) {
        return new RefreshToken(null, userId, tokenHash, issuedAt, expiresAt, null, null);
    }

    public static RefreshToken restore(Long id,
                                       Long userId,
                                       String tokenHash,
                                       LocalDateTime issuedAt,
                                       LocalDateTime expiresAt,
                                       LocalDateTime revokedAt,
                                       Long replacedByTokenId) {
        return new RefreshToken(id, userId, tokenHash, issuedAt, expiresAt, revokedAt, replacedByTokenId);
    }

    public RefreshToken revoke(LocalDateTime revokedAt, Long replacedByTokenId) {
        return new RefreshToken(this.id, this.userId, this.tokenHash, this.issuedAt, this.expiresAt,
                Objects.requireNonNull(revokedAt, "Revoke date is required"), replacedByTokenId);
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public LocalDateTime issuedAt() {
        return issuedAt;
    }

    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    public LocalDateTime revokedAt() {
        return revokedAt;
    }

    public Long replacedByTokenId() {
        return replacedByTokenId;
    }

    private static String requireTokenHash(String raw) {
        String normalized = Objects.requireNonNull(raw, "Refresh token hash is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Refresh token hash cannot be blank");
        }
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Refresh token hash cannot exceed 255 characters");
        }
        return normalized;
    }
}
