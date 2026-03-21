package com.chepamotos.domain.model;

import java.util.Objects;

public final class Mechanic {

    private final Long id;
    private final String name;
    private final boolean active;

    private Mechanic(Long id, String name, boolean active) {
        this.id = id;
        this.name = normalizeName(name);
        this.active = active;
    }

    public static Mechanic createNew(String name) {
        return new Mechanic(null, name, true);
    }

    public static Mechanic restore(Long id, String name, boolean active) {
        return new Mechanic(id, name, active);
    }

    public Mechanic withStatus(boolean active) {
        return new Mechanic(this.id, this.name, active);
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean active() {
        return active;
    }

    private static String normalizeName(String raw) {
        String normalized = Objects.requireNonNull(raw, "Mechanic name is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Mechanic name cannot be blank");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Mechanic name cannot exceed 100 characters");
        }
        return normalized;
    }
}
