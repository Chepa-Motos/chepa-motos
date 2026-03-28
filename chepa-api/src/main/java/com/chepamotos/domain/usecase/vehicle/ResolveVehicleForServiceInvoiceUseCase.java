package com.chepamotos.domain.usecase.vehicle;

import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.VehicleRepository;

import java.util.Locale;
import java.util.Objects;

/**
 * Pure domain use case (strict clean architecture): no Spring annotations/imports.
 */
public final class ResolveVehicleForServiceInvoiceUseCase {

    private final VehicleRepository vehicleRepository;

    public ResolveVehicleForServiceInvoiceUseCase(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle execute(String rawPlate, String rawModel) {
        String normalizedPlate = normalizePlate(rawPlate);
        String normalizedModel = normalizeModel(rawModel);

        return vehicleRepository.findByPlate(normalizedPlate)
                .map(existing -> vehicleRepository.save(existing.withModel(normalizedModel)))
                .orElseGet(() -> vehicleRepository.save(Vehicle.createNew(normalizedPlate, normalizedModel)));
    }

    private String normalizePlate(String rawPlate) {
        String normalized = Objects.requireNonNull(rawPlate, "Vehicle plate is required")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vehicle plate cannot be blank");
        }

        if (normalized.length() > 20) {
            throw new IllegalArgumentException("Vehicle plate cannot exceed 20 characters");
        }

        return normalized;
    }

    private String normalizeModel(String rawModel) {
        String normalized = Objects.requireNonNull(rawModel, "Vehicle model is required")
                .trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vehicle model cannot be blank");
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Vehicle model cannot exceed 100 characters");
        }

        return normalized;
    }
}
