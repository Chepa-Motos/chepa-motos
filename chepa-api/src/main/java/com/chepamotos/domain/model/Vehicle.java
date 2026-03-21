package com.chepamotos.domain.model;

import java.util.Locale;
import java.util.Objects;

public final class Vehicle {

    private final Long id;
    private final String plate;
    private final String model;

    private Vehicle(Long id, String plate, String model) {
        this.id = id;
        this.plate = normalizePlate(plate);
        this.model = normalizeModel(model);
    }

    public static Vehicle createNew(String plate, String model) {
        return new Vehicle(null, plate, model);
    }

    public static Vehicle restore(Long id, String plate, String model) {
        return new Vehicle(id, plate, model);
    }

    public Vehicle withModel(String model) {
        return new Vehicle(this.id, this.plate, model);
    }

    public Long id() {
        return id;
    }

    public String plate() {
        return plate;
    }

    public String model() {
        return model;
    }

    private static String normalizePlate(String raw) {
        String normalized = Objects.requireNonNull(raw, "Vehicle plate is required").trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vehicle plate cannot be blank");
        }
        if (normalized.length() > 20) {
            throw new IllegalArgumentException("Vehicle plate cannot exceed 20 characters");
        }
        return normalized;
    }

    private static String normalizeModel(String raw) {
        String normalized = Objects.requireNonNull(raw, "Vehicle model is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vehicle model cannot be blank");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Vehicle model cannot exceed 100 characters");
        }
        return normalized;
    }
}
