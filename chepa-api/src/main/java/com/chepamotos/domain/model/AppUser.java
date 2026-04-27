package com.chepamotos.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AppUser {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final boolean active;
    private final LocalDateTime createdAt;

    private AppUser(Long id, String username, String passwordHash, UserRole role, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.username = normalizeUsername(username);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.role = Objects.requireNonNull(role, "User role is required");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "User creation date is required");
    }

    public static AppUser createNew(String username, String passwordHash, UserRole role, LocalDateTime createdAt) {
        return new AppUser(null, username, passwordHash, role, true, createdAt);
    }

    public static AppUser restore(Long id, String username, String passwordHash, UserRole role, boolean active, LocalDateTime createdAt) {
        return new AppUser(id, username, passwordHash, role, active, createdAt);
    }

    public AppUser withPasswordHash(String passwordHash) {
        return new AppUser(this.id, this.username, passwordHash, this.role, this.active, this.createdAt);
    }

    public AppUser withStatus(boolean active) {
        return new AppUser(this.id, this.username, this.passwordHash, this.role, active, this.createdAt);
    }

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserRole role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    private static String normalizeUsername(String raw) {
        String normalized = Objects.requireNonNull(raw, "Username is required").trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Username cannot exceed 100 characters");
        }
        return normalized;
    }

    private static String requirePasswordHash(String raw) {
        String normalized = Objects.requireNonNull(raw, "Password hash is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Password hash cannot exceed 255 characters");
        }
        return normalized;
    }
}
