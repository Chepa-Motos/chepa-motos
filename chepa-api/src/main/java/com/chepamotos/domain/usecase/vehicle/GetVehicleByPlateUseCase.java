package com.chepamotos.domain.usecase.vehicle;

import com.chepamotos.domain.exception.VehicleNotFoundException;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.VehicleRepository;

import java.util.Locale;
import java.util.Objects;

/**
 * Pure domain use case (strict clean architecture): no Spring annotations/imports.
 */
public final class GetVehicleByPlateUseCase {

    private final VehicleRepository vehicleRepository;

    public GetVehicleByPlateUseCase(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle execute(String rawPlate) {
        String normalizedPlate = normalizePlate(rawPlate);
        return vehicleRepository.findByPlate(normalizedPlate)
                .orElseThrow(() -> new VehicleNotFoundException(normalizedPlate));
    }

    static String normalizePlate(String rawPlate) {
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
}
